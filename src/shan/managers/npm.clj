(ns shan.managers.npm
  (:require
   [clojure.java.shell :as shell]
   [shan.managers.util :as util]))

(set! *warn-on-reflection* true)

(defn install [pkgs verbose?]
  (util/install-all
   pkgs #(shell/sh "npm" "install" "--global" %) verbose?))

(defn delete [pkgs verbose?]
  (util/delete-all
   pkgs #(shell/sh "npm" "uninstall" "--global" %) verbose?))
