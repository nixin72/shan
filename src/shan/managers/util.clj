(ns shan.managers.util
  (:require
   [clojure.java.shell :as shell]))

(defn install-all [pkgs install-fn]
  (mapv (fn [p]
          (when-not (nil? p)
            (println "Installing" p "...")
            (let [out (install-fn (str p))]
              (if (= (:exit out) 0)
                (println "Successfully installed!")
                (println "Failed to install"))
              (hash-map p out))))
        pkgs))

(defn delete-all [pkgs delete-fn]
  (mapv (fn [p]
          (when-not (nil? p)
            (println "Uninstalling" p "...")
            (let [out (delete-fn (str p))]
              (if (= (:exit out) 0)
                (println "Successfully uninstalled!")
                (println "Failed to uninstall"))
              (hash-map p out))))
        pkgs))
