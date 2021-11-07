(ns shan.link
  (:import [java.nio.file Files Paths FileSystems FileAlreadyExistsException]
           [java.nio.file.attribute FileAttribute]
           [java.io File])
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [shan.util :as u :refer [suppress-stdout]]))

(defn create-path [path paths]
  (Paths/get path (into-array String paths)))

(defn path [path & paths]
  (let [path (.toString path)
        paths (map #(.toString %) paths)]
    (cond
      (or (= path "~")
          (.startsWith path (str "~" File/separator)))
      (create-path (str (System/getProperty "user.home") (subs path 1)) paths)

      (.startsWith path "~")
      (u/fatal-error "Home directory expansion not implemented for explicit usernames."
                     "\nPlease use the full path to the directory instead.")

      :else
      (create-path path paths))))

(defn last-segment [path]
  (-> path .iterator iterator-seq last))

(defn create-links [links]
  (mapv #(try
           (Files/createSymbolicLink
            (path (first %))
            (path (second %))
            (make-array FileAttribute 0))
           (println (u/blue "Linking")
                    (.toString (second %)) "to" (.toString (first %)))
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

(defn link-structures [src dest ignore-files]
  (doseq [f (-> src Files/list .iterator iterator-seq)]
    (let [appended-dest (.toAbsolutePath (path dest (last-segment f)))]
      (cond
        ;; Skip anything matching the .shanignore + .git
        (walk/walk #(.matches % f) #(some true? %) ignore-files)
        (println (u/yellow "Skipping") (.toString f))

        ;; Recurse if it's a directory
        (is-dir? f)
        (if (exists? appended-dest)
          (link-structures f (path dest (last-segment f)) ignore-files)
          (create-links {appended-dest f}))

        ;; Just make a symlink
        :else
        (create-links {appended-dest f})))))

(defn parse-shanignore [path]
  (->> (path path ".shanignore")
       (.toString)
       (slurp)
       (str/split-lines)
       (#(conj % ".git"))         ; Add .git so it's always ignored
       (map #(.getPathMatcher
              (FileSystems/getDefault) (str "glob:**/" %)))))

(defn cli-link-dotfiles [{:keys [check _arguments]}]
  (let [[src dest] (map #(path %) _arguments)
        ignore-files (parse-shanignore src)]
    (suppress-stdout
     (not check)
     (link-structures src dest ignore-files))
    (println "Done.")))
