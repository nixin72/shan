(ns shan.print
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.string :as str]))

(def ^:dynamic *mout* *out*)
(def exit-code (atom 0))
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

(defn identity-prn [arg]
  (prn arg)
  arg)

(defn identity-pprint [arg]
  (pprint arg)
  arg)

(defn sprintln [& args]
  (print (str (str/join " " args) "\n"))
  (flush))

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
     (let [time (.format (java.text.SimpleDateFormat. "hh:mm:ss") (java.util.Date.))]
       (print (str (log-time status)
                   (str/join " " args)
                   (if newline? "\n" "")))
       (flush)))))

(defn log [& args] (force-print *out* :success args))
(defn logln [& args] (force-print *out* :success args true))

(defn warn [& args] (force-print *out* :warning args))
(defn warnln [& args] (force-print *out* :warning args true))

(defn loading [start-msg task]
  (let [sym (cycle "|/-\\")
        kill (future
               (binding [*out* (new java.io.StringWriter)]
                 [(task) (.toString *out*)]))]
    (loop [sym sym]
      (if (future-done? kill)
        (let [[ret out] @kill]
          (print (str "\r" start-msg " " out))
          ret)                          ; RETURNS HERE lmao
        (do (Thread/sleep 100)
            (print (str "\r" start-msg " " (first sym)))
            (flush)
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
