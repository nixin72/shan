(ns shan.list
  (:require
   [clojure.string :as str]
   [shan.util :as u]))

(defn cli-list [{:keys [temp _arguments]}]
  (letfn [(print-config [config]
            (->> (dissoc config :default-manager)
                 (reduce-kv
                  (fn [a k v]
                    (conj a (str (u/bold (name k)) ":\n" (str/join ", " v))))
                  [])
                 (str/join "\n\n")
                 println))
          (config-empty []
            (println (str "You have no" (if temp " temporary " " ") "packages installed.")))]
    (cond
      (not= _arguments []) (println "Too many arguments supplied to list")

      (= temp false)
      (let [conf (u/get-new)]
        (if (= conf {})
          (config-empty)
          (print-config conf)))

      :else
      (let [conf (u/get-temp)]
        (if (= conf {})
          (config-empty)
          (print-config conf))))
    (u/exit-code)))
