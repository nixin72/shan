(ns shan.managers.npm
  (:require [clojure.java.shell :as shell]))

(set! *warn-on-reflection* true)

(defn install [pkgs]
  (apply str "npm install" pkgs)
  #_(:exit (apply shell/sh "/bin/sh" "-c" "npm" "install" "--global" pkgs)))

(defn delete [pkgs]
  (apply str "npm install" pkgs)
  #_(:exit (apply shell/sh "/bin/sh" "-c" "npm" "uninstall" "--global" pkgs)))
