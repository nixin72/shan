(ns shan.config
  (:require
   [clojure.java.io :as io]))

(defn file-exists? [file-path]
  (-> file-path io/file .exists))

(defn create-file
  ([file-path] (create-file file-path "{}"))
  ([file-path contents]
   (-> file-path java.io.File. .createNewFile)
   (spit file-path contents)
   (println "Creating file: " file-path)))

(def ^:dynamic home (str (System/getenv "HOME") "/"))
(def local ".local/share/")
(def config ".config/")
(def appdata "AppData/Local/")

(def OS (System/getProperty "os.name"))
(def unix? (some #{OS} #{"Linux" "Mac OS X"}))
(def windows? (not unix?))

(def ^:dynamic gen-dir
  (case OS
    ("Linux" "Mac OS X")
    (let [dir (str home local "shan/")]
      (when-not (file-exists? dir)
        (-> dir java.io.File. .mkdir))
      dir)

    ;; Windows
    (let [dir (str home appdata "shan/")]
      (when-not (file-exists? dir)
        (-> dir java.io.File. .mkdir))
      dir)))

(def ^:dynamic conf-dir
  (case OS
    ("Linux" "Mac OS X")
    (cond
      (file-exists? (str home ".shan.edn")) home
      (file-exists? (str home config "shan.edn")) (str home ".config/")
      (file-exists? (str home config "shan/shan.edn")) (str home config "shan/")
      :else
      ;; If no config file exists, go ahead and create one and let the user know.
      (do (create-file (str home config "shan.edn"))
          (str home config)))
    ;; Windows
    (cond
      (file-exists? (str home ".shan.edn")) home
      (file-exists? (str home appdata "shan.edn")) (str home appdata)
      (file-exists? (str home appdata "shan/shan.edn")) (str home appdata "shan/")
      :else
      ;; If no config file exists, go ahead and create one and let the user know.
      (do (create-file (str home appdata "shan.edn"))
          (str home appdata)))))

(def ^:dynamic conf-file (str conf-dir "shan.edn"))
(def ^:dynamic temp-file (str gen-dir "temporary.edn"))
(def ^:dynamic gen-file (str gen-dir "generations.edn"))

(when-not (file-exists? temp-file)
  (create-file temp-file))

(when-not (file-exists? gen-file)
  (create-file gen-file "[{}]"))
