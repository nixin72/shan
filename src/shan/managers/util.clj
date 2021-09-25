(ns shan.managers.util
  (:require
   [shan.util :as u]))

(defn install-all [pkgs install-fn installed? verbose?]
  (->>
   ;; Put it into a set first to avoid doing the same thing multiple times
   (into #{} pkgs)
   ;; Filter out all packages that are already installed or any nil values
   (filter
    #(cond (installed? %) (println (u/bold %) (u/blue "is already installed"))
           (nil? %) false
           :else true))
   ;; Install all other packages
   (mapv (fn [p]
           (-> (str "Installing " p "... ") u/bold print)
           (flush)
           (let [out (install-fn (str p))]
             (when verbose?
               (println "\n" (:out out)))
             (if (= (:exit out) 0)
               (do (-> "Successfully installed!" u/green println) p)
               (-> "Failed to install" u/red println)))))))

(defn delete-all [pkgs delete-fn installed? verbose?]
  (->>
   ;; Avoid doing the same thing twice
   (into #{} pkgs)
   ;; Filter out packages that aren't installed - no need to "uninstall" them
   (filter
    #(cond
       (not (installed? %)) (println (u/bold %) (u/blue "does not exist"))
       (nil? %) false
       :else true))
   ;; Uninstall all other packages
   (mapv (fn [p]
           (-> (str "Uninstalling " p "... ") u/bold print)
           (flush)
           (let [out (delete-fn (str p))]
             (when verbose?
               (println "\n" (:out out)))
             (if (= (:exit out) 0)
               (-> "Successfully uninstalled!" u/green println)
               (-> "Failed to uninstall" u/red println))
             (into {:package p} out))))))
