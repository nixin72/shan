(ns shan.list
  (:require
   [clojure.pprint :as pprint]
   [clojure.string :as str]
   [clojure.data.json :as json]
   [shan.print :as p]
   [shan.util :as u]
   [shan.managers :as pm]))

(defn ignore-keys [config]
  (dissoc config :default-manager :links))

(defn print-human [config]
  (->> (ignore-keys config)
       (reduce-kv
        (fn [a k v]
          (conj a (str (p/bold (name k)) ":\n"
                       (if (map? v)
                         (str/join ", " (map #(str (% 0) "=" (% 1)) v))
                         (str/join ", " v)))))
        [])
       (str/join "\n\n")
       p/sprintln))

(defn print-parseable [config]
  (->> (ignore-keys config)
       (reduce-kv
        (fn [_ k v]
          (if (map? v)
            (mapv #(p/sprintln (name k) (% 0) (% 1)) v)
            (mapv #(p/sprintln (name k) %1) v)))
        nil)
       (into [])))

(defn cli-list [{:keys [temp format _arguments]}]
  (letfn [(print-config [config]
            (if (= config {})
              (p/sprintln (str "You have no" (if temp " temporary " " ") "packages installed."))
              (case format
                ("human" nil) (print-human config)
                "parse" (print-parseable config)
                "edn" (pprint/pprint config)
                "json" (json/pprint config))))]

    (if (empty? _arguments)
      (if-not temp
        (print-config (u/get-new))
        (print-config (u/get-temp)))

      ;; TODO: Make this speeby
      (->> _arguments
           (mapv #(reduce (fn [a v] (assoc a v [%])) {} (pm/installed-with %)))
           (apply u/merge-conf)
           prn))

    @p/exit-code))
