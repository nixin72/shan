(ns shan.commands.purge
  (:require
   [shan.print :as p]
   [shan.config :refer [app-config]]
   [shan.util :as u]
   [shan.packages :as ps]
   [shan.commands.-options :as opts]))

(defn- cli-purge
  "Deletes all packages in temporary.edn file"
  [{:keys [check]}]
  (let [remove-map (u/get-temp)
        result (reduce-kv #(assoc %1 %2 (ps/remove-pkgs %2 %3 check))
                          {}
                          remove-map)]
    (spit (:temp-file @app-config) "{}")
    (if (:testing? @app-config) result p/exit-code)))

(def command
  {:command "purge"
   :short "pg"
   :category "Managing Packages"
   :arguments? 0
   :description "Purges all temporary packages from your system"
   :opts [opts/check?]
   :runs cli-purge})
