(ns shan.utils.symlinks
  (:import [java.nio.file Files FileAlreadyExistsException]
           [java.nio.file.attribute FileAttribute])
  (:require
   [clojure.walk :as walk]
   [shan.utils.paths :as paths]
   [shan.print :as p]))

(defn create-links [links]
  (mapv #(try
           (Files/createSymbolicLink
            (paths/path (first %))
            (paths/path (second %))
            (make-array FileAttribute 0))
           (p/logln (p/blue "Linking")
                    (.toString (second %)) "to" (.toString (first %)))
           (catch FileAlreadyExistsException _
             (p/error "Failed to make link to" (str (first %) ":")
                      "File already exists.")))
        links))

(defn remove-links [links]
  (mapv #(Files/deleteIfExists (paths/path %)) links))

(defn link-structures [src dest ignore-files]
  (doseq [f (-> src Files/list .iterator iterator-seq)]
    (let [appended-dest (.toAbsolutePath (paths/path dest (paths/last-segment f)))]
      (cond
        ;; Skip anything matching the .shanignore + .git
        (walk/walk #(.matches % f) #(some true? %) ignore-files)
        (p/logln (p/yellow "Skipping") (.toString f))

        ;; Recurse if it's a directory
        (paths/is-dir? f)
        (if (paths/exists? appended-dest)
          (link-structures f (paths/path dest (paths/last-segment f)) ignore-files)
          (create-links {appended-dest f}))

        ;; Just make a symlink
        :else
        (create-links {appended-dest f})))))
