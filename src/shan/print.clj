(ns shan.print
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]))

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

(def ^:dynamic *mout* *out*)
(def exit-code (atom 0))
(def allow-printing (atom true))
(def print-queue (atom clojure.lang.PersistentQueue/EMPTY))

(defn flush-print-queue
  ([] (flush-print-queue *out*))
  ([output-stream]
   (when (and (seq @print-queue) @allow-printing)
     (with-redefs [*out* output-stream]
       (print (peek @print-queue))
       (flush)
       (swap! print-queue pop)
       (flush-print-queue)))))

(defn sprint [& args]
  (let [print-string (str/join " " args)]
    (swap! print-queue conj print-string)
    (flush-print-queue)))

(defn sprintln [& args]
  (let [print-string (str (str/join " " args) "\n")]
    (swap! print-queue conj print-string)
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

(defn loading [start-msg task]
  (let [sym (cycle "|/-\\")
        kill (future (task))]
    (loop [sym sym]
      (when-not (future-done? kill)
        (Thread/sleep 100)
        (println "\033[1A")
        (print (str start-msg " " (first sym)))
        (flush)
        (recur (rest sym))))
    (print "\b")
    (flush)
    @kill))

(defn error [& args]
  (reset! exit-code 1)
  (force-print *err* :error args))

(defn errorln [& args]
  (reset! exit-code 1)
  (force-print *err* :error args true))

(defn fatal-error [& args]
  (apply errorln args)
  (System/exit @exit-code))

(defn log [& args] (force-print *mout* :success args))
(defn logln [& args] (force-print *mout* :success args true))

(defn warn [& args] (force-print *mout* :warning args))
(defn warnln [& args] (force-print *mout* :warning args true))
