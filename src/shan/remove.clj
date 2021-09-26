(ns shan.remove
  (:require
   [clojure.string :as str]
   [shan.util :as u]
   [shan.managers.managers :as pm]))

(defn remove-with-pm-from-list [pms pkg]
  (try
    (println
     (str "Which package manager(s) would you like to remove "
          (u/blue pkg)  " using?\n"
          (->> (conj pms :all)
               (map-indexed (fn [k v] (str "- " (name v) " (" k ")")))
               (str/join "\n"))))
    (let [which (Integer/parseInt (read-line))
          pm (get pms which)]
      (if pm
        {pm [pkg]}
        (reduce #(assoc %1 %2 [pkg]) {} pms)))
    (catch java.lang.NumberFormatException _
      (u/error "Please enter a number"))))

(defn remove-with-pm-from-installed [pkg]
  (let [pms (pm/installed-with pkg)]
    (cond
      (= (count pms) 0)
      (u/warning (str "Package '" (u/bold pkg)
                      "' isn't installed in any package manager known by Shan."))

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
        delete-map (u/flat-map->map _arguments :default)
        default (find-package-manager (dissoc conf :default)
                                      (:default delete-map))
        delete-map (dissoc delete-map :default)
        delete-map (u/merge-conf default delete-map)]
    (reduce-kv #(assoc %1 %2 (pm/delete %2 %3 verbose)) {} delete-map)
    (if temp
      (u/remove-from-temp conf delete-map)
      (u/remove-from-new conf delete-map))))
