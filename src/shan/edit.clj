(ns shan.edit
  (:require
   [shan.print :as p]
   [shan.config :as c]
   [shan.util :as u]
   [shan.managers :as pm]))

(defn cli-edit
  "Launches a subprocess to edit your shan.edn file."
  [& _]
  (u/sh-verbose (System/getenv "EDITOR") c/conf-file)
  p/exit-code)

(defn cli-purge
  "Deletes all packages in temporary.edn file"
  [{:keys [check]}]
  (let [remove-map (u/get-temp)
        result (reduce-kv #(assoc %1 %2 (pm/remove-pkgs %2 %3 check))
                          {}
                          remove-map)]
    (spit c/temp-file "{}")
    (if c/testing? result p/exit-code)))

(defn cli-merge
  "Adds all packages in temporary.edn to shan.edn file."
  [{:keys []}]
  (let [temp (u/get-temp)]
    (u/add-to-conf temp)
    (spit c/temp-file "{}")
    (if c/testing? temp p/exit-code)))

(defn cli-default
  "Sets the :default-manager key in the shan.edn file"
  [{:keys [_arguments]}]
  (let [conf (u/get-new)
        default (first _arguments)]
    (cond
      (nil? default)
      (p/error (str "Argument expected. Use shan default --help for details."))

      (contains? pm/package-managers (keyword default))
      (u/write-edn c/conf-file (assoc conf :default-manager (keyword default)))

      :else
      (p/error "Package manager" (p/bold default) "is not known by shan."))
    (if c/testing? default @p/exit-code)))
