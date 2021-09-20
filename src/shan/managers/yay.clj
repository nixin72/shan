(ns shan.managers.yay
  (:require
   [clojure.string :as str]
   [clojure.java.shell :as shell]))

(set! *warn-on-reflection* true)

(defn install [pkgs]
  (prn (str "yay install " (str/join pkgs)))
  (:exit (apply shell/sh "/bin/sh" "-c" "yay" "-S" :in pkgs)))

(defn delete [pkgs]
  (prn (str "yay remove " (str/join pkgs)))
  (:exit (apply shell/sh "/bin/sh" "-c" "yay" "-R" :in pkgs)))
