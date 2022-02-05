(ns shan.utils.paths
  (:import [java.nio.file Paths]
           [java.io File])
  (:require [shan.print :as p]))

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
      (p/fatal-error "Home directory expansion not implemented for explicit usernames."
                     "\nPlease use the full path to the directory instead.")

      :else
      (create-path path paths))))

(defn last-segment [path]
  (-> path .iterator iterator-seq last))

(defn exists? [path]
  (-> path .toFile .exists))

(defn is-dir? [path]
  (-> path .toFile .isDirectory))
