(ns shan.managers.yay
  (:require
   [clojure.java.shell :as shell]
   [shan.managers.util :as util]))

(set! *warn-on-reflection* true)

(defn install [pkgs verbose?]
  (util/install-all
   pkgs #(shell/sh "yay" "-S" "--noconfirm" %) verbose?))

(defn delete [pkgs verbose?]
  (util/delete-all
   pkgs #(shell/sh "yay" "-R" "--noconfirm" %) verbose?))

(defn installed? [pkg]
  (= 0 (:exit (shell/sh "yay" "-Q" (str pkg)))))
