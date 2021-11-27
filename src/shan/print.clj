(ns shan.print
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]
   [clojure.java.shell :as sh]))

(defn identity-prn [arg]
  (prn arg)
  arg)

(defn identity-pprint [arg]
  (pprint arg)
  arg)

(def normal "\033[0m")

(defn color [color strs]
  (str color (str/join " " strs) normal))

(defn red [& string]    (color "\033[0;31m" string))
(defn green [& string]  (color "\033[0;32m" string))
(defn yellow [& string] (color "\033[0;33m" string))
(defn blue [& string]   (color "\033[0;34m" string))
(defn purple [& string] (color "\033[0;35m" string))
(defn grey [& string]   (color "\033[0;37m" string))
(defn bold [& string]   (color "\033[0;1m"  string))

(defn save-cursor-position [] (print "\0337"))
(defn reserve-bottom-line [lines] (print (str "\033[0;" (dec lines) "r")))
(defn restore-cursor-position [] (print "\0338"))
(defn move-up-line [] (print "\033[1A"))
(defn move-to-bottom [lines] (print (str "\033[" lines ";0f")))
(defn free-bottom-line [lines] (print (str "\033[0;" lines "r")))
(defn clear-line [] (print "\033[0K"))

(def ^:dynamic *mout* *out*)
(def exit-code (atom 0))
(def allow-printing (atom true))
(def print-queue (atom clojure.lang.PersistentQueue/EMPTY))

(defn sprint [& args]
  (let [print-string (str/join " " args)]
    (if @allow-printing
      (do
        (print print-string)
        (flush))
      (swap! print-queue conj print-string))))

(defn sprintln [& args]
  (let [print-string (str (str/join " " args))]
    (if @allow-printing
      (println print-string)
      (do
        (println print-string)
        (swap! print-queue conj print-string)))))

(defn flush-print-queue []
  (when-not (empty? @print-queue)
    (print (peek @print-queue))
    (flush)
    (swap! print-queue pop)
    (flush-print-queue)))

(defn log-time [status]
  (let [time (.format (java.text.SimpleDateFormat. "hh:mm:ss") (java.util.Date.))]
    (->>
     (str "[" time "] ")
     ((case status
        :success green
        :warning yellow
        :error red))
     bold)))

(defn force-print
  ([out status args] (force-print out status args false))
  ([out status args newline?]
   (binding [*out* out]
     (print (str (log-time status)
                 (str/join " " args)
                 (if newline? "\n" "")))
     (flush))))

(defmacro with-excursion [& body]
  `(do
     (reset! allow-printing false)
     (save-cursor-position)
     ~@body
     (restore-cursor-position)
     (reset! allow-printing true)))

(defn loading [start-msg task]
  (let [sym (cycle "|/-\\")
        lines (-> (sh/sh "tput" "lines") :out str/trim-newline Integer/parseInt)
        kill (future (task))]
    (newline)
    (with-excursion
      (reserve-bottom-line lines))
    (move-up-line)
    (loop [sym sym]
      (if (future-done? kill)
        (let [ret @kill]
          (with-excursion
            (free-bottom-line lines)
            (move-to-bottom lines)
            (clear-line))
          (newline)
          ;; NOTE: returns from here lmao
          ret)
        (do
          (Thread/sleep 100)
          (flush-print-queue)
          (with-excursion
            (move-to-bottom lines)
            (print (str start-msg " " (first sym)))
            (flush))
          (newline)
          #_(println @print-queue)
          #_(flush-print-queue)
          (recur (rest sym)))))))

(defn error [& args]
  (reset! exit-code 1)
  (force-print *err* :error args))

(defn errorln [& args]
  (reset! exit-code 1)
  (force-print *err* :error args true))

(defn fatal-error [& args]
  (apply errorln args)
  (System/exit @exit-code))

(defn log [& args] (force-print *out* :success args))
(defn logln [& args] (force-print *out* :success args true))

(defn warn [& args] (force-print *out* :warning args))
(defn warnln [& args] (force-print *out* :warning args true))
