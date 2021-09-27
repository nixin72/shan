(ns shan.managers.npm
  (:require
   [clojure.java.shell :as shell]
   [clojure.java.io :as io]
   [shan.util :as u]))

(def node-path
  (let [path (System/getenv "NODE_PATH")
        warn "Not having $NODE_PATH set may cause shan to reinstall NPM packages."]
    (cond
      (nil? path)
      (u/warning (str "Your NODE_PATH is not set.\n" warn))

      (not (.exists (io/file path)))
      (u/warning (str "Your NODE_PATH does not exist.\n" warn))

      :else path)))

(defn installed? [pkg]
  (if node-path
    (some #{(str pkg)} (->> node-path io/file .list (mapv identity)))
    (shell/sh "npm" "list" "-g" (str pkg))))
