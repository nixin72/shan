(ns shan.commands.-options
  (:require
   [shan.print :as p]))

(def check?
  {:as "Lets package managers take over stdin/stdout"
   :default false
   :option "check"
   :short "c"
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

(def batch?
  {:as "Perfoms the operation in batch. Good for performance."
   :default false
   :option "batch"
   :short "b"
   :type :with-flag})

(def parallel?
  {:as "Installs packages from various managers in parallel."
   :default false
   :option "parallel"
   :short "p"
   :type :with-flag})

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

(def context
  (str " Given the following shan.edn config file:\n"
       (p/red " {") (p/blue ":default-manager :yay") "\n"
       (p/blue "  :yay") " " (p/yellow "[") "nodejs python3 neovim atop" (p/yellow "]") "\n"
       (p/blue "  :npm") " " (p/yellow "[") "atop react" (p/yellow "]") (p/red "}") "\n\n"))
