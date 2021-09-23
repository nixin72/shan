(ns shan.remove
  (:require
   [shan.util :as u]
   [clojure.string :as str]))

(defn in-config [conf pkg]
  (let [pkg (symbol pkg)]
    (reduce-kv (fn [a k v]
                 (if (some #{pkg} v)
                   (assoc a k [pkg])
                   a))
               {} conf)))

(defn find-package-manager [conf pkgs]
  (mapv (fn [pkg]
          (let [pkg (symbol pkg)
                pms (reduce-kv
                     (fn [a k v] (if (some #{pkg} v)
                                   (conj a k) a))
                     []
                     conf)]
            (if (> (count pms) 1)
              (do
                (println
                 (str "Which package manager(s) would you like to remove this package using?\n"
                      (->> (conj pms :all)
                           (map-indexed (fn [k v] (str "- " (name v) " (" k ")")))
                           (str/join "\n"))))
                (let [which (read-line)]
                  (println which)))
              (conj pms pkg))))
        pkgs))

(defn cli-remove [{:keys [verbose temp _arguments]}]
  (let [conf (if temp (u/get-temp) (u/get-new))
        remove (u/flat-map->map _arguments :default)
        default (find-package-manager conf (:default remove))]
    (println default)
    (println (map #(in-config conf %) _arguments))))
