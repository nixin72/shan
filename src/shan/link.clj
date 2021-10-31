(ns shan.link
  (:import [java.nio.file Files Path Paths FileAlreadyExistsException]
           [java.nio.file.attribute FileAttribute])
  (:require
   [shan.util :as u :refer [suppress-stdout]]))

(defn path [path & paths]
  (Paths/get (if (string? path) path (.toString path))
             (if (empty? paths)
               (make-array String 0)
               (into-array
                String (map #(if (string? %) % (.toString %)) paths)))))

(defn last-segment [path]
  (-> path .iterator iterator-seq last))

(defn create-links [links]
  (mapv #(try
           (Files/createSymbolicLink
            (path (first %))
            (path (second %))
            (make-array FileAttribute 0))
           (println "Linking" (.toString (second %)) "to" (.toString (first %)))
           (catch FileAlreadyExistsException _
             (u/error "Failed to make link to" (str (first %) ":")
                      "File already exists.")))
        links))

(defn remove-links [links]
  (mapv #(Files/deleteIfExists (path %)) links))

(defn cli-link [{:keys [_arguments]}]
  (let [links (->> _arguments
                   (partition 2)
                   (reduce (fn [a [src dest]] (assoc a dest src)) {}))
        new-conf (update (u/get-new) :links merge links)]

    (create-links links)
    (u/write-to-conf new-conf)))

(defn cli-unlink [{:keys [_arguments]}]
  (let [links _arguments
        conf (update (u/get-new) :links #(apply dissoc % (map symbol links)))]

    (remove-links links)
    (u/write-to-conf conf)))

(defn exists? [path]
  (-> path .toFile .exists))
(defn is-dir? [path]
  (-> path .toFile .isDirectory))

(defn link-structures [src dest]
  (let [dir-contents (-> src Files/list .iterator iterator-seq)]
    (doseq [f dir-contents]
      (let [appended-dest (path dest (last-segment f))]
        (cond
          (.endsWith f ".git")          ; Always ignore .git
          nil
          (is-dir? f)                   ; Recurse if it's a directory
          (if (exists? appended-dest)
            (link-structures f (path dest (last-segment f)))
            (create-links {appended-dest f}))

          :else                         ; Make the symlink
          (create-links {appended-dest f}))))))

(defn cli-link-dotfiles [{:keys [verbose _arguments]}]
  (let [[src dest] (map #(path %) _arguments)]
    (suppress-stdout
     (not verbose)
     (link-structures src dest))))
