(ns shan.managers.pip
  (:require
   [clojure.java.shell :as shell]
   [shan.util :as u]))

(defn installed? [pkg]
  (= 0 (:exit (shell/sh "python" "-m" "pip" "show" (str pkg)))))

(defn install [pkgs verbose?]
  (u/install-all
   pkgs #(shell/sh "python" "-m" "pip" "install" %) installed? verbose?))

(defn delete [pkgs verbose?]
  (u/delete-all
   pkgs #(shell/sh "python" "-m" "pip" "uninstall" "-y" %) installed? verbose?))
