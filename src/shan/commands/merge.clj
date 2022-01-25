(ns shan.commands.merge
  (:require
   [shan.print :as p]
   [shan.config :as c]
   [shan.util :as u]))

(defn- cli-merge
  "Adds all packages in temporary.edn to shan.edn file."
  [{:keys []}]
  (let [temp (u/get-temp)]
    (u/add-to-conf temp)
    (spit c/temp-file "{}")
    (if c/testing? temp p/exit-code)))

(def command
  {:command "merge"
   :short "mg"
   :category "Managing Packages"
   :arguments? 0
   :description "Merges all temporary packages into your config file"
   :runs cli-merge})
