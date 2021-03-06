(ns shan.commands.default
  (:require [shan.util :as u]
            [shan.print :as p]
            [shan.managers :as pm]
            [shan.config :as c :refer [app-config]]))

(defn- cli-default
  "Sets the :default-manager key in the shan.edn file"
  [{:keys [_arguments]}]
  (let [conf (u/get-new)
        default (first _arguments)]
    (cond
      (nil? default)
      (println (or (name (:default-manager conf))
                   "No default package manager has been set."))

      (contains? pm/package-managers (keyword default))
      (u/write-edn (:conf-file @app-config) (assoc conf :default-manager (keyword default)))

      :else
      (p/error "Package manager" (p/bold default) "is not known by shan."))
    (if c/testing? default @p/exit-code)))

(def command
  {:command "default"
   :short "de"
   :category "Managing Packages"
   :arguments? "<package-manager>"
   :description "Change the default package manager in your config"
   :runs cli-default})
