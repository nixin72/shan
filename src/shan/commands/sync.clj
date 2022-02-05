(ns shan.commands.sync
  (:require
   [clojure.data :as data]
   [shan.print :as p]
   [shan.util :as u]
   [shan.config :as c]
   [shan.managers :as pm]
   [shan.commands.-options :as opts]))

(defn- cli-sync [{:keys [check]}]
  (let [old-config (u/get-old)
        last-config (last old-config)
        new-config (dissoc (u/get-new) :default-manager)
        [add del] (data/diff new-config last-config)
        del (dissoc del :default-manager)]

    (when-not (= [add del] [nil nil])
      (u/add-generation new-config))

    (if c/testing?
      [(reduce-kv #(assoc %1 %2 (when %3 (pm/install-pkgs %2 %3 check))) {} add)
       (reduce-kv #(assoc %1 %2 (when %3 (pm/remove-pkgs %2 %3 check))) {} del)]
      p/exit-code)))

(def command
  {:command "sync"
   :short "sc"
   :arguments? 0
   :description "Syncs your config to get your system up to date"
   :desc-long ["\n - Installs and removes any packages that have been changed in your config"
               "\n - Adds and removes symlinks to files"
               "\n - Installs packages from non-package manager sources"
               "\n - Runs any scripts specified"]
   :runs cli-sync
   :opts [opts/check?]})
