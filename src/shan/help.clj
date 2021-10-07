(ns shan.help
  (:require
   [clojure.string :as str]
   [shan.util :as u]
   [shan.managers :as pm]))

(defn build-name [cmd subcmd desc]
  (str "\n" (u/bold "NAME:") "\n " cmd (if subcmd (str " " subcmd) "") " - " desc "\n"))

(defn build-usage [cmd long short options?]
  (str "\n" (u/bold "USAGE:") "\n " cmd " "
       (cond (and long short) (str "[" long "|" short "]")
             long long
             short short) " "
       (and options? "[command-options] ")
       "[arguments...]\n"))

(defn build-global-usage [cmd]
  (str "\n" (u/bold "USAGE:") "\n " cmd
       " [global-options] command [command-options] [arguments...]\n"))

(defn build-version [version]
  (str "\n" (u/bold "VERSION:") "\n " version "\n"))

(defn build-command [long short desc]
  (format "   %-2s, %-10s %s\n" short long desc))

(defn build-commands [commands]
  (apply str "\n" (u/bold "COMMANDS:") "\n"
         (map #(build-command (:command %) (:short %) (:description %)) commands)))

(defn build-opt [long short desc]
  (format "   -%s, --%-8s %s\n" short long desc))

(defn build-opts [opts]
  (apply str "\n" (u/bold "OPTIONS:") "\n"
         (map #(build-opt (:option %) (:short %) (:as %))
              (conj opts {:option "help" :short "?" :as ""}))))

(defn build-example [desc ex]
  (format "   $ %s\n     # %s\n" ex (u/grey desc)))

(defn build-examples [examples]
  (let [context (if (string? (first examples)) (first examples) false)]
    (str "\n" (u/bold "EXAMPLES:") "\n"
         (or context "")
         (str/join "\n" (map #(build-example (:desc %) (:ex %))
                             (if context (rest examples) examples))))))

(defn build-pms [managers]
  (str "\n" (u/bold "PACKAGE MANAGERS:") "\n "
       (str/join ", " (sort (map name managers))) "\n"))

(defn global-help [conf _]
  (print (str (build-name (:command conf) nil (:description conf))
              (build-global-usage (:command conf))
              (build-version (:version conf))
              (build-commands (:subcommands conf))
              (build-opts (:opts conf))))
  (flush))

(defn subcommand-help [setup [cmd subcommand]]
  (let [info (first (filter #(or (= (:command %) subcommand)
                                 (= (:short %) subcommand))
                            (:subcommands setup)))]
    (print (str (build-name cmd subcommand (:description info))
                (build-usage cmd (:command info) (:short info) (:opts info))
                (if (some #{subcommand} #{"install" "in" "remove" "rm" "sync" "sc"})
                  (build-pms (keys (pm/installed-managers)))
                  "")
                (build-opts (:opts info))
                (if (:examples info) (build-examples (:examples info)) "")))
    (flush)))
