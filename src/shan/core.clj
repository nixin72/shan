(ns shan.core
  (:require
   #_[cli-matic.core :refer [run-cmd]]
   [shan.parser :refer [run-cmd]]
   [shan.edit :as edit]
   [shan.install :as install]
   [shan.list :as list]
   [shan.remove :as remove]
   [shan.rollback :as rollback]
   [shan.config :as c]
   [shan.sync :as sync]
   [shan.help :as help]
   [shan.init :as init]
   [shan.archive :as archive]
   [shan.util :as u])
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

(def editor
  {:as "Specify an editor to use instead of $EDITOR"
   :default (System/getenv "EDITOR")
   :option "editor"
   :short "e"
   :type :string})

(def output-format
  {:as "Specify an output format. Options are human, json, edn, and parseable"
   :default "human"
   :option "format"
   :short "f"
   :type #{"human" "json" "parse" "edn"}})

(def context
  (str " Given the following shan.edn config file:\n"
       (u/red " {") (u/blue ":default-manager :yay") "\n"
       (u/blue "  :yay") " " (u/yellow "[") "nodejs python3 neovim atop" (u/yellow "]") "\n"
       (u/blue "  :npm") " " (u/yellow "[") "atop react" (u/yellow "]") (u/red "}") "\n\n"))

(def help
  {:as "Show this help page"
   :default false
   :option "help"
   :short "h"
   :type :with-flag})

(def version
  {:as "Display software version"
   :default false
   :option "version"
   :short "v"
   :type :with-flag})

(def config
  {:command "shan"
   :description "A declarative wrapper around your favourite package manager"
   :version c/version
   :global-help help/global-help
   :subcmd-help help/subcommand-help
   :global-opts [help version]
   :subcommands
   [{:command "default"
     :short "de"
     :category "Managing Packages"
     :arguments? "<package-manager>"
     :description "Change the default package manager in your config"
     :runs edit/cli-default}
    {:command "install"
     :short "in"
     :category "Managing Packages"
     :arguments? *
     :description "Install packages through any supported package manager"
     :examples [context
                {:desc (str "Install packages through " (u/blue "yay") ", this config's default package manager")
                 :ex (str (u/green "shan") " install open-jdk vscode")}
                {:desc "Install packages through a specified package manager"
                 :ex (str (u/green "shan") " install " (u/blue ":npm") " react-native expo")}
                {:desc "Install packages through various package managers"
                 :ex (str (u/green "shan") " install open-jdk vscode " (u/blue ":npm") " react-native expo " (u/blue ":pip") " PyYAML")}]
     :runs install/cli-install
     :opts [verbose? temporary?]}
    {:command "remove"
     :short "rm"
     :category "Managing Packages"
     :arguments? *
     :description "Uninstall packages through any supported package manager"
     :examples [context
                {:desc "Removes the packages through yay"
                 :ex (str (u/green "shan") " remove python nodejs")}
                {:desc "Removes neovim through yay and react through npm"
                 :ex (str (u/green "shan") " remove neovim react")}
                {:desc "Removes atop after prompting the user which manager to use."
                 :ex (str (u/green "shan") " remove atop")}
                {:desc "Removes emacs after searching to find out what installed it."
                 :ex (str (u/green "shan") " remove emacs")}]
     :runs remove/cli-remove
     :opts [verbose? temporary?]}
    {:command "add-archive"
     :short "ap"
     :category "Managing Packages"
     :arguments *
     :description "Adds package archives for your package managers to use"
     :runs archive/cli-add-ppa
     :opts [verbose? temporary?]}
    {:command "del-archive"
     :short "dp"
     :category "Managing Packages"
     :arguments *
     :description "Removes package archives from your package managers"
     :runs archive/cli-del-ppa
     :opts [verbose? temporary?]}
    {:command "sync"
     :short "sc"
     :arguments? 0
     :description "Syncs your config to get your system up to date"
     :desc-long ["Installs and removes any packages that have been changed in your config"
                 "Adds and removes symlinks to files"
                 "Installs packages from non-package manager sources"
                 "Runs any scripts specified"]
     :runs sync/cli-sync
     :opts [verbose?]}
    {:command "rollback"
     :short "rb"
     :arguments 0
     :description "Roll back your last change"
     :runs rollback/cli-rollback}
    {:command "edit"
     :short "ed"
     :arguments? 0
     :description "Shells out to a text editor for you to edit your config"
     :opts [editor]
     :runs edit/cli-edit}
    {:command "purge"
     :short "pg"
     :category "Managing Packages"
     :arguments? 0
     :description "Purges all temporary packages from your system"
     :opts [verbose?]
     :runs edit/cli-purge}
    {:command "merge"
     :short "mg"
     :category "Managing Packages"
     :arguments? 0
     :description "Merges all temporary packages into your config file"
     :runs edit/cli-merge}
    {:command "list"
     :short "ls"
     :category "Managing Packages"
     :arguments? 0
     :description "Lists all of the packages installed through Shan"
     :runs list/cli-list
     :opts [temporary? output-format]}
    {:command "gen"
     :short "ge"
     :arguments? 0
     :description "Generates a config for first time use from installed packages"
     :opts [editor]
     :runs init/cli-init}
    {:command "version"
     :short "v"
     :arguments 0
     :description "Print the current version of shan"
     :runs (fn [_] (println c/version))}]})

(defn -main [& args]
  (run-cmd ["install" "--help" "-v" "-t" "npm" "-npm" "react"] config)
  #_(try
      (case (first args)
      ;; If it looks like the user is trying to get help, help them
        ("h" "help" "-h" "--help") (run-cmd ["--help"] config)
      ;; Not providing -v and --version breaks a user's general expectations
      ;; since they're pretty standard. Yes, they're also provided with
      ;; v and version subcommands, but *not* having this would break expectations.
        ("-v" "--version") (run-cmd ["version"] config)
        nil
        (run-cmd ["help"] config)
      ;; Otherwise, just do what they want
        (run-cmd args config))
      (catch clojure.lang.ExceptionInfo _
        (run-cmd ["help"] config))))
