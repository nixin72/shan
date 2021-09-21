(ns shan.managers.yay
  (:require
   [clojure.java.shell :as shell]
   [shan.managers.util :as util]))

(set! *warn-on-reflection* true)

(defn install [pkgs]
  (util/install-all pkgs #(shell/sh "yay" "-S" "--noconfirm" %)))

(defn delete [pkgs]
  (util/delete-all pkgs #(shell/sh "yay" "-R" "--noconfirm" %)))
