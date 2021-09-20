(ns shan.managers.pip
  (:require [clojure.java.shell :as shell]))

(set! *warn-on-reflection* true)

(defn install [pkgs]
  (apply str "pip install" pkgs)
  #_(:exit (apply shell/sh "/bin/sh" "-c" "pip" "install" pkgs)))

(defn delete [pkgs]
  (apply str "pip delete" pkgs)
  #_(:exit (apply shell/sh "/bin/sh" "-c" "pip" "uninstall" pkgs)))
