(ns shan.managers.npm
  (:require
   [clojure.java.shell :as shell]
   [shan.managers.util :as util]))

(set! *warn-on-reflection* true)

(defn install [pkgs]
  (util/install-all pkgs #(shell/sh "npm" "install" "--global" %)))

(defn delete [pkgs]
  (util/delete-all pkgs #(shell/sh "npm" "uninstall" "--global" %)))
