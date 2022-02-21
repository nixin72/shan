(ns shan.core
  (:require
   [shan.parser :refer [run-cmd]]
   [shan.print :as p]
   [shan.config :as c]
   [shan.cache :as cache]
   [shan.commands.-options :as opts]
   [shan.commands.add-archive :as add-archive]
   [shan.commands.default :as default]
   [shan.commands.edit :as edit]
   [shan.commands.help :as help]
   [shan.commands.init :as init]
   [shan.commands.install :as install]
   [shan.commands.link-dots :as link-dots]
   [shan.commands.link :as link]
   [shan.commands.list :as list]
   [shan.commands.merge :as merge]
   [shan.commands.purge :as purge]
   [shan.commands.regen-cache :as regen-cache]
   [shan.commands.remove :as remove]
   [shan.commands.remove-archive :as remove-archive]
   [shan.commands.rollback :as rollback]
   [shan.commands.sync :as sync]
   [shan.commands.unlink :as unlink])
  (:gen-class))

(def config
  {:command "shan"
   :description "A declarative wrapper around your favourite package manager"
   :version c/version
   :global-help help/global-help
   :subcmd-help help/subcommand-help
   :global-opts [opts/help opts/version]
   :subcommands
   [default/command
    install/command
    remove/command
    add-archive/command
    remove-archive/command
    sync/command
    rollback/command
    link/command
    unlink/command
    link-dots/command
    edit/command
    purge/command
    regen-cache/command
    merge/command
    list/command
    init/command]})

(defn -main [& args]
  (if (= "root" (System/getProperty "user.name"))
    (p/fatal-error "Do not run as root.")
    (try
      (future (cache/read-cache))
      (case (first args)
        ;; If it looks like the user is trying to get help, help them
        ("h" "help" "-h" "--help")
        (run-cmd ["--help"] config)
        ;; Also help them if they provide no arguments
        nil
        (run-cmd ["--help"] config)
        ;; Otherwise, just do what they want
        (run-cmd args config))
      (catch clojure.lang.ExceptionInfo _
        (run-cmd ["--help"] config))))

  (shutdown-agents)
  (System/exit @p/exit-code))

(comment
  "Some tests:"
  (-main "install" "htop")
  (-main "install" "htop" "atop")
  (-main "remove" "htop")

  "end test")
