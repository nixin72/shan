(ns shan.managers.yay
  (:require
   [clojure.string :as str]
   [clojure.java.shell :as shell]))

(set! *warn-on-reflection* true)

(defn install [pkgs]
  (:exit (shell/sh "yay" "-S" "--noconfirm" (str/join " " pkgs))))

(defn delete [pkgs]
  (:exit (shell/sh "yay" "-R" "--noconfirm" (str/join " " pkgs))))
