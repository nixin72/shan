(ns shan.config
  (:require
   [clojure.java.io :as io]))

(def version "0.7.0")

(defn file-exists? [file-path]
  (-> file-path io/file .exists))

(defn create-file
  ([file-path] (create-file file-path "{}"))
  ([file-path contents]
   (-> file-path java.io.File. .createNewFile)
   (spit file-path contents)
   (println "Creating file: " file-path)))

(def ^:dynamic testing? false)
(def ^:dynamic home (str (System/getenv "HOME") "/"))
(def local ".local/share/")
(def cache ".cache/")
(def appdata "AppData/Local/")

(def OS (System/getProperty "os.name"))
(def unix? (some #{OS} #{"Linux" "Mac OS X"}))
(def windows? (not unix?))

(defn build-path [& path-elements]
  (let [dir (->> path-elements
                 (mapv #(cond (string? %) %
                              (map? %) (case OS
                                         ("Linux" "Mac OS X") (:unix %)
                                         (:win %))))
                 (apply str))]
    (when-not (file-exists? dir)
      (-> dir java.io.File. .mkdir))
    dir))

(def ^:dynamic data-dir
  (build-path home {:unix local :win appdata} "shan/"))

(def ^:dynamic cache-dir
  (build-path home {:unix cache :win appdata} "shan/"))

(def ^:dynamic conf-file (str data-dir "shan.edn"))
(def ^:dynamic temp-file (str data-dir "temporary.edn"))
(def ^:dynamic gen-file (str data-dir "generations.edn"))
(def ^:dynamic cache-file (str cache-dir "cache.edn"))

(when-not (file-exists? temp-file)
  (create-file temp-file))

(when-not (file-exists? gen-file)
  (create-file gen-file "[{}]"))
