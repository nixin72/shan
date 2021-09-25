(ns shan.managers.managers
  (:require
   [shan.managers.yay :as yay]
   [shan.managers.npm :as npm]
   [shan.managers.pip :as pip]))

(defn install [manager pkgs verbose?]
  (case manager
    :yay (yay/install pkgs verbose?)
    :pip (pip/install pkgs verbose?)
    :npm (npm/install pkgs verbose?)))

(defn delete [manager pkgs verbose?]
  (case manager
    :yay (yay/delete pkgs verbose?)
    :pip (pip/delete pkgs verbose?)
    :npm (npm/delete pkgs verbose?)))

(defn installed-with [pkg]
  (filterv #(case %
              :yay (yay/installed? pkg)
              :pip (pip/installed? pkg)
              :npm (npm/installed? pkg))
           [:yay :pip :npm]))
