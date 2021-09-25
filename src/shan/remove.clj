(ns shan.remove
  (:require
   [shan.util :as u]
   [clojure.string :as str]
   [clojure.data :as data]
   [shan.managers.managers :as pm]))

(defn in-config [conf pkg]
  (let [pkg (symbol pkg)]
    (reduce-kv (fn [a k v]
                 (if (some #{pkg} v)
                   (assoc a k [pkg])
                   a))
               {} conf)))

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
    (println pms)
    (cond
      (= (count pms) 0)
      (u/error "This package isn't installed in any package manager known by Shan.")

      (= (count pms) 1) {(first pms) [pkg]}
      :else (remove-with-pm-from-list pms pkg))))

(defn find-package-manager [conf pkgs]
  (apply
   u/merge-conf
   (mapv (fn [pkg]
           (let [pkg (symbol pkg)
                 pms (reduce-kv
                      (fn [a k v] (if (some #{pkg} v)
                                    (conj a k) a))
                      []
                      conf)]
             (cond
               (= (count pms) 1) {(first pms) [pkg]}
               (= (count pms) 0) (remove-with-pm-from-installed pkg)
               :else (remove-with-pm-from-list pms pkg))))
         pkgs)))

(defn cli-remove [{:keys [verbose temp _arguments]}]
  (let [conf (if temp (u/get-temp) (u/get-new))
        remove (u/flat-map->map _arguments :default)
        default (find-package-manager conf (:default remove))]
    (println remove)
    (println (data/diff conf default))
    #_(println (map #(in-config conf %) _arguments))))

;; (cli-remove {:_arguments ["atop" "nvm"]})
