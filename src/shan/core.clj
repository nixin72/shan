(ns shan.core
  (:require
   [cli-matic.core :refer [run-cmd]]
   [shan.edit :as edit]
   [shan.install :as install]
   [shan.list :as list]
   [shan.remove :as remove]
   [shan.rollback :as rollback]
   [shan.sync :as sync]
   [shan.help :as help]
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

(def context
  (str " Given the following shan.edn config file:\n"
       (u/red " {") (u/blue ":default-manager :yay") "\n"
       (u/blue "  :yay") " " (u/yellow "[") "nodejs python3 neovim atop" (u/yellow "]") "\n"
       (u/blue "  :npm") " " (u/yellow "[") "atop react" (u/yellow "]") (u/red "}") "\n\n"))

(def opts
  {:command "shan"
   :description "A declarative wrapper around your favourite package manager"
   :version "0.5.0"
   :global-help help/global-help
   :subcmd-help help/subcommand-help
   :subcommands
   [{:command "sync"
     :short "sc"
     :arguments? false
     :description "Syncs your config to your installed packages."
     :runs sync/cli-sync
     :opts [verbose?]}
    {:command "rollback"
     :short "rb"
     :description "Roll back your last change."
     :runs rollback/cli-rollback}
    {:command "install"
     :short "in"
     :arguments? true
     :description "Install packages through any supported package manager."
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
     :arguments? true
     :description "Uninstall packages through any supported package manager."
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
    {:command "list"
     :short "ls"
     :arguments? false
     :description "Lists all of the packages installed through Shan."
     :runs list/cli-list
     :opts [temporary?]}
    {:command "config"
     :short "cf"
     :description "A variety of tools for managing your config."
     :subcommands
     [{:command "edit"
       :short "ed"
       :description "Shells out to $EDITOR for you to edit your config"
       :runs edit/cli-edit}
      {:command "purge"
       :short "pg"
       :description "Removes all temporary packages."
       :runs edit/cli-purge}
      {:command "merge"
       :short "mg"
       :description "Merges all temporary packages into your config file."
       :runs edit/cli-merge}]}]})

(defn -main [& args]
  (let [result
        (try
          (case (first args)
            ;; If it looks like the user is trying to get help, help them
            ("h" "help" "-h" "doc" "docs" "info" "?") (run-cmd ["--help"] opts)
            (run-cmd args opts))
          (catch clojure.lang.ExceptionInfo _
            (run-cmd ["--help"] opts)))]
    (when (= result [{} {}])
      (println "No changes made."))
    (shutdown-agents)))
