(ns shan.remove
  (:require
   [clojure.java.shell]
   [shan.util :as u]
   [shan.config :as c]
   [shan.managers :as pm]))

(defn remove-with-pm-from-list [pms pkg]
  (let [pm (u/prompt (str "Which package manager(s) would you like to remove "
                          (u/blue pkg)  " using?\n")
                     pms)]
    (if pm {pm [pkg]} (reduce #(assoc %1 %2 [pkg]) {} pms))))

(defn remove-with-pm-from-installed [pkg]
  (let [pms (pm/installed-with pkg)]
    (cond
      (= (count pms) 0)
      (do (u/warning (str "Package '" (u/bold pkg)
                          "' isn't installed in any package manager known by Shan."))
          {})

      (= (count pms) 1) {(first pms) [pkg]}
      :else (remove-with-pm-from-list pms pkg))))

(defn find-package-manager [conf pkgs]
  (apply
   u/merge-conf
   (mapv (fn [pkg]
           (let [pkg (symbol pkg)
                 pms (reduce-kv #(if (some #{pkg} %3) (conj %1 %2) %1) [] conf)]
             (cond
               (= (count pms) 1) {(first pms) [pkg]}
               (= (count pms) 0) (remove-with-pm-from-installed pkg)
               :else (remove-with-pm-from-list pms pkg))))
         pkgs)))

(defn cli-remove [{:keys [verbose temp _arguments]}]
  (let [conf (if temp (u/get-temp) (u/get-new))
        remove-map (u/flat-map->map _arguments :default)
        default (find-package-manager (dissoc conf :default-manager)
                                      (:default remove-map))
        remove-map (dissoc remove-map :default)
        remove-map (u/merge-conf default remove-map)
        out (reduce-kv #(assoc %1 %2 (pm/remove-pkgs %2 %3 verbose))
                       {}
                       remove-map)]
    (if temp
      (u/remove-from-temp conf remove-map)
      (u/remove-from-conf conf remove-map))
    (if c/testing? out u/exit-code)))
