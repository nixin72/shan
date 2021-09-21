(ns shan.managers.util)

(defn install-all [pkgs install-fn verbose?]
  (mapv (fn [p]
          (when-not (nil? p)
            (println "Installing" p "...")
            (let [out (install-fn (str p))]
              (when verbose? (println (:out out)))
              (if (= (:exit out) 0)
                (println "Successfully installed!")
                (println "Failed to install"))
              (hash-map p out))))
        pkgs))

(defn delete-all [pkgs delete-fn verbose?]
  (mapv (fn [p]
          (when-not (nil? p)
            (println "Uninstalling" p "...")
            (let [out (delete-fn (str p))]
              (when verbose? (println (:out out)))
              (if (= (:exit out) 0)
                (println "Successfully uninstalled!")
                (println "Failed to uninstall"))
              (hash-map p out))))
        pkgs))
