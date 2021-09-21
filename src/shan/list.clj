(ns shan.list
  (:require
   [clojure.string :as str]
   [shan.util :as u]))

(defn cli-list [{:keys [temp _arguments]}]
  (letfn [(print-config [config]
            (->> config
                 (reduce-kv
                  (fn [a k v]
                    (conj a
                          (str "\033[1m" (name k) ":\033[0m"
                               "\n" (str/join ", " v))))
                  [])
                 (str/join "\n\n")
                 println))]
    (cond
      (not= _arguments []) (println "Too many arguments supplied to list")
      (= temp false) (print-config (u/get-new))
      :else (print-config (u/get-temp)))))
