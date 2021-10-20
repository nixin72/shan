(ns shan.list
  (:require
   [clojure.pprint :as pprint]
   [clojure.string :as str]
   [clojure.data.json :as json]
   [shan.util :as u]))

(defn print-human [config]
  (->> (dissoc config :default-manager)
       (reduce-kv
        (fn [a k v]
          (conj a (str (u/bold (name k)) ":\n"
                       (if (map? v)
                         (str/join ", " (map #(str (% 0) "=" (% 1)) v))
                         (str/join ", " v)))))
        [])
       (str/join "\n\n")
       println))

(defn print-parseable [config]
  (->> (dissoc config :default-manager)
       (reduce-kv
        (fn [_ k v]
          (if (map? v)
            (mapv #(println (name k) (% 0) (% 1)) v)
            (mapv #(println (name k) %1) v)))
        nil)
       (into [])))

(defn cli-list [{:keys [temp format _arguments]}]
  (letfn [(print-config [config]
            (if (= config {})
              (println (str "You have no" (if temp " temporary " " ") "packages installed."))
              (case format
                ("human" nil) (print-human config)
                "parse" (print-parseable config)
                "edn" (pprint/pprint config)
                "json" (json/pprint config))))]
    (cond
      (not= _arguments []) (println "Too many arguments supplied to list")

      (= temp false)
      (let [conf (u/get-new)]
        (print-config conf))

      :else
      (let [conf (u/get-temp)]
        (print-config conf)))
    @u/exit-code))
