(ns shan.managers.pip
  (:require
   [clojure.java.shell :as shell]
   [shan.managers.util :as util]))

(set! *warn-on-reflection* true)

(defn installed? [pkg]
  (= 0 (:exit (shell/sh "python" "-m" "pip" "show" (str pkg)))))

(defn install [pkgs verbose?]
  (util/install-all
   pkgs #(shell/sh "python" "-m" "pip" "install" %) installed? verbose?))

(defn delete [pkgs verbose?]
  (util/delete-all
   pkgs #(shell/sh "python" "-m" "pip" "uninstall" "-y" %) verbose?))
