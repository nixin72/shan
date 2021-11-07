(ns shan.edit
  (:require
   [shan.config :as c]
   [shan.util :as u]
   [shan.managers :as pm]))

(defn cli-edit [& _]
  (u/sh-verbose (System/getenv "EDITOR") c/conf-file)
  u/exit-code)

(defn cli-purge [{:keys [check]}]
  (let [remove-map (u/get-temp)
        result (reduce-kv #(assoc %1 %2 (pm/remove-pkgs %2 %3 check))
                          {}
                          remove-map)]
    (spit c/temp-file "{}")
    (if c/testing? result u/exit-code)))

(defn cli-merge [{:keys []}]
  (let [temp (u/get-temp)]
    (u/add-to-conf temp)
    (spit c/temp-file "{}")
    (if c/testing? temp u/exit-code)))

(defn cli-default [{:keys [_arguments]}]
  (let [conf (u/get-new)
        default (first _arguments)]
    (cond
      (nil? default)
      (u/error (str "Argument expected. Use shan default --help for details."))

      (contains? pm/package-managers (keyword default))
      (u/write-edn c/conf-file (assoc conf :default-manager (keyword default)))

      :else
      (u/error "Package manager" (u/bold default) "is not known by shan."))
    (if c/testing? default @u/exit-code)))
