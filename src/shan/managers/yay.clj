(ns shan.managers.yay
  (:require
   [clojure.java.shell :as shell]
   [shan.util :as u]))

(set! *warn-on-reflection* true)

(defn installed? [pkg]
  (= 0 (:exit (shell/sh "yay" "-Q" (str pkg)))))

(defn install [pkgs verbose?]
  (u/install-all
   pkgs #(shell/sh "yay" "-S" "--noconfirm" %) installed? verbose?))

(defn delete [pkgs verbose?]
  (u/delete-all
   pkgs #(shell/sh "yay" "-R" "--noconfirm" %) installed? verbose?))
