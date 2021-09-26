(ns shan.managers.managers
  (:require
   [shan.managers.yay :as yay]
   [shan.managers.npm :as npm]
   [shan.managers.pip :as pip]
   [shan.util :as u]))

(def package-managers [:npm :pip :yay])

(defn install [manager pkgs verbose?]
  (println "Installing" (u/bold (name manager)) "packages:")
  (let [out (case manager
              :yay (yay/install pkgs verbose?)
              :pip (pip/install pkgs verbose?)
              :npm (npm/install pkgs verbose?))]
    (println "")
    out))

(defn delete [manager pkgs verbose?]
  (println "Uninstalling" (u/bold (name manager)) "packages:")
  (let [out (case manager
              :yay (yay/delete pkgs verbose?)
              :pip (pip/delete pkgs verbose?)
              :npm (npm/delete pkgs verbose?))]
    (println "")
    out))

(defn installed-with [pkg]
  (filterv #(case %
              :yay (yay/installed? pkg)
              :pip (pip/installed? pkg)
              :npm (npm/installed? pkg))
           [:yay :pip :npm]))
