(ns shan.install
  (:require
   [shan.util :as u]
   [shan.managers :as pm]))

(defn generate-success-report [result]
  (reduce-kv (fn [a k v]
               (let [{:keys [s f c]}
                     (reduce #(-> (if (second %2)
                                    (update % :s conj (second %2))
                                    (update % :f conj (second %2)))
                                  (update :c conj (first %2)))
                             {:s [] :f [] :c []}
                             v)]
                 (-> (assoc-in a [:success k] s)
                     (update :commands into c)
                     (assoc :failed (boolean (seq f))))))
             {:success {} :commands #{} :failed false}
             result))

(defn find-default-manager [install-map]
  (if-let [pkgs (get install-map nil)]
    (let [[pm set-default?] (pm/determine-default-manager)]
      (-> install-map
          (dissoc nil)
          (assoc pm pkgs)
          ((fn [x]
             (if set-default? (assoc x :default-manager pm) x)))))
    install-map))

(defn cli-install [{:keys [verbose temp _arguments]}]
  (let [new-conf (u/get-new)
        install-map (u/flat-map->map _arguments (:default-manager new-conf))
        install-map (find-default-manager install-map)
        result (reduce-kv #(assoc %1 %2 (pm/install-pkgs %2 %3 verbose))
                          {}
                          (dissoc install-map :default-manager))
        {:keys [success failed commands]} (generate-success-report result)]
    (when failed
      (println "\nPackages that failed to install were not added to your config."))
    (cond
      temp (u/add-to-temp success)
      (:default-manager install-map)
      (u/add-to-conf
       (assoc new-conf :default-manager (:default-manager install-map))
       success)
      :else (u/add-to-conf new-conf success))
    commands))
