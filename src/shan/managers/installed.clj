(ns shan.managers.installed
  (:require
   [clojure.java.shell :as shell]
   [clojure.java.io :as io]
   [shan.print :as p]
   [shan.util :as u]
   [shan.managers.list :as list]))

(defn gu? [pkg] (some #{pkg} (list/gu)))
(defn raco? [pkg] (some #{pkg} (list/raco)))

(def warned? (atom false))
(def node-path (System/getenv "NODE_PATH"))

(defn npm? [pkg]
  ;; Only warn them the node-path is not set if:
  ;; 1. They try actually using NPM
  ;; 2. They haven't already been warned yet in this use of shan
  (when-not @warned?
    (let [warn "Not having $NODE_PATH set may cause shan to reinstall NPM packages."
          path (cond
                 (nil? node-path)
                 (p/warnln (str "Your NODE_PATH is not set.\n" warn))

                 (not (.exists (io/file node-path)))
                 (p/warnln (str "Your NODE_PATH does not exist.\n" warn))

                 :else node-path)]
      (when-not path
        (reset! warned? false))))
  (if node-path
    (some #{(str pkg)} (->> node-path io/file .list (mapv identity)))
    (do
      (println)
      (shell/sh "npm" "list" "-g" (str pkg)))))
