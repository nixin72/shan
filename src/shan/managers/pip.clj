(ns shan.managers.pip
  (:require
   [clojure.java.shell :as shell]
   [shan.managers.util :as util]))

(set! *warn-on-reflection* true)

(defn install [pkgs verbose?]
  (util/install-all
   pkgs #(shell/sh "python" "-m" "pip" "install" %) verbose?))

(defn delete [pkgs verbose?]
  (util/delete-all
   pkgs #(shell/sh "python" "-m" "pip" "uninstall" "-y" %) verbose?))

(defn installed? [pkg]
  (= 0 (:exit (shell/sh "python" "-m" "pip" "show" (str pkg)))))
