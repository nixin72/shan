(ns shan.managers.managers
  (:require
   [shan.managers.yay :as yay]
   [shan.managers.npm :as npm]
   [shan.managers.pip :as pip]))

(set! *warn-on-reflection* true)

(defn install [manager pkgs]
  (case manager
    :yay (yay/install pkgs)
    :pip (pip/install pkgs)
    :npm (npm/install pkgs)))

(defn delete [manager pkgs]
  (case manager
    :yay (yay/delete pkgs)
    :pip (pip/delete pkgs)
    :npm (npm/delete pkgs)))
