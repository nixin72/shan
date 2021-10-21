(ns shan.help
  (:require
   [clojure.string :as str]
   [shan.util :as u]
   [shan.managers :as pm]))

(defn build-name [cmd subcmd desc]
  (str "\n" (u/bold "NAME:") "\n " cmd (if subcmd (str " " subcmd) "") " - " desc "\n"))

(defn build-description [desc]
  (apply str "\n" (map #(format " - %s\n" %) desc)))

(defn build-usage [cmd long short options? arguments?]
  (str "\n" (u/bold "USAGE:") "\n " cmd " "
       (cond (and long short) (str "[" long "|" short "]")
             long long
             short short) " "
       (and options? "[command-options] ")
       (cond
         (= arguments? *) "[arguments...]"
         (vector? arguments?) (str "[" (first arguments?) "]")
         (string? arguments?) arguments?
         :else "")
       "\n"))

(defn build-global-usage [cmd]
  (str "\n" (u/bold "USAGE:") "\n " cmd
       " command [global-options] [command-options] [arguments...]\n"))

(defn build-version [version]
  (str "\n" (u/bold "VERSION:") "\n " version "\n"))

(defn build-command [cmd]
  (format "\n    %-2s, %-10s %s" (:short cmd) (:command cmd) (:description cmd)))

(defn build-commands [commands]
  (->> commands
       (group-by :category)
       (reduce-kv
        (fn [a k v]
          (conj a (str "  " (u/bold (if (nil? k) "Other" k)) ":"
                       (apply str (map (fn [x] (build-command x)) v))
                       "\n")))
        [])
       (str/join "\n")
       (str "\n" (u/bold "COMMANDS:") "\n")))

(defn build-opt [long short desc]
  (format "   -%s, --%-8s %s\n" short long desc))

(defn build-opts [opts]
  (->> (conj opts {:as "Print the help pages" :option "help" :short "h"})
       (map #(build-opt (:option %) (:short %) (:as %)))
       (apply str "\n" (u/bold "OPTIONS:") "\n")))

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
              (build-opts (conj (:opts conf)
                                {:as "Print the current version of shan"
                                 :option "version"
                                 :short "v"}))))
  (flush)
  u/exit-code)

(defn subcommand-help [setup [cmd subcommand]]
  (let [info (first (filter #(or (= (:command %) subcommand)
                                 (= (:short %) subcommand))
                            (:subcommands setup)))]
    (print (str (build-name cmd subcommand (:description info))
                (if (:desc-long info) (build-description (:desc-long info)) "")
                (build-usage cmd (:command info) (:short info) (:opts info) (:arguments? info))
                (if (some #{subcommand} #{"install" "in" "remove" "rm" "sync" "sc"})
                  (build-pms (keys (pm/installed-managers)))
                  "")
                (if (:subcommands info) (build-commands (:subcommands info)) "")
                (build-opts (:opts info))
                (if (:examples info) (build-examples (:examples info)) "")))
    (flush)
    u/exit-code))
