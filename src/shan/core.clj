(ns shan.core
  (:require
   [cli-matic.core :refer [run-cmd]]
   [shan.managers.npm :as npm]
   [shan.edit :as edit]
   [shan.install :as install]
   [shan.list :as list]
   [shan.remove :as remove]
   [shan.rollback :as rollback]
   [shan.sync :as sync])
  (:gen-class))

(def verbose?
  {:as "Performs the action verbosely."
   :default false
   :option "verbose"
   :short "v"
   :type :with-flag})

(def temporary?
  {:as "Don't reflect the changes in the config."
   :default false
   :option "temp"
   :short "t"
   :type :with-flag})

(def opts
  {:command "shan"
   :description "A declarative wrapper around your favourite package manager"
   :version "0.1.0"
   :subcommands
   [{:command "sync"
     :short "sc"
     :description "Syncs your config to your installed packages."
     :runs sync/sync-conf
     :opts [verbose?]}
    {:command "rollback"
     :short "rb"
     :description "Roll back your last change."
     :runs rollback/cli-rollback}
    {:command "install"
     :short "in"
     :description "Installs a package through a specified package manager."
     :runs install/cli-install
     :opts [verbose? temporary?]}
    {:command "remove"
     :short "rm"
     :description "Uninstalls a package and removes it from your configuration."
     :runs remove/cli-remove
     :opts [verbose? temporary?]}
    {:command "list"
     :short "ls"
     :description "Lists all of the packages installed through Shan."
     :runs list/cli-list
     :opts [temporary?]}
    {:command "edit"
     :short "ed"
     :description "Shells out to $EDITOR for you to edit your config"
     :runs edit/cli-edit}]})

(defn -main [& args]
  (let [result (run-cmd args opts)]
    (when (= result [{} {}])
      (println "No changes made."))
    (shutdown-agents)))
