(ns shan.config
  (:require
   [clojure.java.io :as io]
   [clojure.edn :as edn]))

(defn file-exists? [file-path]
  (-> file-path io/file .exists))

(defn create-file
  ([file-path] (create-file file-path "{}"))
  ([file-path contents]
   (-> file-path java.io.File. .createNewFile)
   (spit file-path contents)
   (println "Creating file: " file-path)))

(defn build-path [OS & path-elements]
  (->> path-elements
       (mapv #(cond (string? %) %
                    (map? %) (case OS
                               ("Linux" "Mac OS X") (:unix %)
                               (:win %))))
       (apply str)))

(def ^:dynamic testing? false)
(def version (->> "deps.edn" slurp .getBytes io/reader java.io.PushbackReader. edn/read :version))
(def local ".local/share/")
(def cache ".cache/")
(def appdata "AppData/Local/")

(def app-config
  (delay
   (let [conf {:home (str (System/getenv "HOME") "/")
               :os (System/getProperty "os.name")}]
     (as-> conf $
       (assoc $ :unix? (some #{(:os $)} #{"Linux" "Mac OS X"}))
       (assoc $ :windows? (not (:unix? $)))
       (assoc $ :data-dir (build-path (:os $) (:home $) {:unix local :win appdata} "shan/"))
       (assoc $ :cache-dir (build-path (:os $) (:home $) {:unix cache :win appdata} "shan/"))
       (assoc $ :conf-file (str (:data-dir $) "shan.edn"))
       (assoc $ :temp-file (str (:data-dir $) "temporary.edn"))
       (assoc $ :gen-file (str (:data-dir $) "generations.edn"))
       (assoc $ :cache-file (str (:data-dir $) "cache.edn"))))))

(defn setup-first-time-use []
  ;; Make directories
  (when-not (file-exists? (:data-dir @app-config))
    (-> (:data-dir @app-config) java.io.File. .mkdirs))
  (when-not (file-exists? (:cache-dir @app-config))
    (-> (:cache-dir @app-config) java.io.File. .mkdirs))

  ;; Make files
  (when-not (file-exists? (:conf-file @app-config))
    (create-file (:conf-file @app-config) "{}"))
  (when-not (file-exists? (:temp-file @app-config))
    (create-file (:temp-file @app-config) "{}"))
  (when-not (file-exists? (:gen-file @app-config))
    (create-file (:gen-file @app-config) "[{}]"))
  (when-not (file-exists? (:cache-file @app-config))
    (create-file (:cache-file @app-config) "{}")))
