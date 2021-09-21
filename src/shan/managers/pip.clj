(ns shan.managers.pip
  (:require
   [clojure.java.shell :as shell]
   [shan.managers.util :as util]))

(set! *warn-on-reflection* true)

(defn install [pkgs]
  (util/install-all pkgs #(shell/sh "python" "-m" "pip" "install" %)))

(defn delete [pkgs]
  (util/delete-all pkgs #(shell/sh "python" "-m" "pip" "uninstall" "-y" %)))
