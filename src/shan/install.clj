(ns shan.install
  (:require
   [shan.print :as p]
   [shan.util :as u]
   [shan.managers :as pm]
   [shan.config :as c]))

(defn generate-success-report
  "Used to check which package installations succeeded or failed.
  Returns a map of successful installations, the commands used for those
  installations, and a boolean to indicate if there were failures."
  [result]
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

(defn find-default-manager
  "Returns a package map"
  [install-map]
  (if-let [pkgs (get install-map nil)]
    (let [[pm set-default?] (pm/determine-default-manager)]
      (-> install-map
          (dissoc nil)
          (assoc pm pkgs)
          ((fn [x]
             (if set-default? (assoc x :default-manager pm) x)))))
    install-map))

(defn cli-install
  "Tries to install all packages specified in _arguments using the correct package manager"
  [{:keys [check temp _arguments]}]
  (let [new-conf (u/get-new)
        install-map (u/flat-map->map _arguments (:default-manager new-conf))
        install-map (find-default-manager install-map)
        result (reduce-kv #(assoc %1 %2 (pm/install-pkgs %2 %3 check))
                          {}
                          (dissoc install-map :default-manager))
        {:keys [success failed commands]} (generate-success-report result)
        success (pm/replace-keys success)]
    (when failed
      (p/error "\nPackages that failed to install were not added to your config."))

    (cond
      temp (u/add-to-temp success)
      (:default-manager install-map)
      (u/add-to-conf
       (assoc new-conf :default-manager (:default-manager install-map))
       success)
      :else (u/add-to-conf new-conf success))
    (if c/testing? commands p/exit-code)))
