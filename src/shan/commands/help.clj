(ns shan.commands.help
  (:require
   [clojure.string :as str]
   [shan.print :as p]
   [shan.managers :as pm]))

(defn build-name [cmd subcmd desc]
  (str "\n" (p/bold "DESCRIPTION:") "\n " cmd (if subcmd (str " " subcmd) "") " - " desc "\n"))

(defn build-description [desc]
  (str (str/join " " desc) "\n"))

(defn build-usage [cmd long short options? arguments]
  (str "\n" (p/bold "USAGE:") "\n " cmd " "
       (cond (and long short) (str "[" long "|" short "]")
             long long
             short short) " "
       (and options? "[command-options] ")
       (cond
         (= arguments *) "[arguments...]"
         (vector? arguments) (str/join " " (map #(str "<" % ">") arguments))
         (string? arguments) arguments
         :else "")
       "\n"))

(defn build-global-usage [cmd]
  (str "\n" (p/bold "USAGE:") "\n " cmd
       " command [global-options] [command-options] [arguments...]\n"))

(defn build-version [version]
  (str "\n" (p/bold "VERSION:") "\n " version "\n"))

(defn build-command [{:keys [short command description]}]
  (if (nil? short)
    (format "\n        %-11s %s" command description)
    (format "\n    %s, %-11s %s" short command description)))

(defn build-commands [commands]
  (->> commands
       (group-by :category)
       (into (sorted-map))
       (reverse)
       (reduce
        (fn [a [k v]]
          (conj a (str "  " (p/bold (if (nil? k) "Other" k)) ":"
                       (apply str (map (fn [x] (build-command x)) v))
                       "\n")))
        [])
       (str/join "\n")
       (str "\n" (p/bold "COMMANDS:") "\n")))

(defn build-opt [long short desc]
  (format "   -%s, --%-8s %s\n" short long desc))

(defn build-opts [opts]
  (->> (conj opts {:as "Print the help pages" :option "help" :short "h"})
       (map #(build-opt (:option %) (:short %) (:as %)))
       (apply str "\n" (p/bold "OPTIONS:") "\n")))

(defn build-example [desc ex]
  (format "   $ %s\n     # %s\n" ex desc))

(defn build-examples [examples]
  (let [context (if (string? (first examples)) (first examples) false)]
    (str "\n" (p/bold "EXAMPLES:") "\n"
         (or context "")
         (str/join "\n" (map #(build-example (:desc %) (:ex %))
                             (if context (rest examples) examples))))))

(defn build-pms [managers]
  (str "\n" (p/bold "PACKAGE MANAGERS:") "\n "
       (str/join ", " (sort (map name managers))) "\n"))

(defn global-help [conf]
  (print (str (build-name (:command conf) nil (:description conf))
              (build-global-usage (:command conf))
              (build-version (:version conf))
              (build-commands (:subcommands conf))
              (build-opts (conj (:opts conf)
                                {:as "Print the current version of shan"
                                 :option "version"
                                 :short "v"}))))
  (flush)
  p/exit-code)

(defn subcommand-help [setup [cmd subcommand]]
  (let [info (first (filter #(or (= (:command %) subcommand)
                                 (= (:short %) subcommand))
                            (:subcommands setup)))]
    (print (str (build-name cmd subcommand (:description info))
                (if (:desc-long info) (build-description (:desc-long info)) "")
                (build-usage cmd (:command info) (:short info) (:opts info) (:arguments info))
                (if (some #{subcommand} #{"install" "in" "remove" "rm" "sync" "sc"})
                  (build-pms (keys (pm/installed-managers)))
                  "")
                (if (:subcommands info) (build-commands (:subcommands info)) "")
                (build-opts (:opts info))
                (if (:examples info) (build-examples (:examples info)) "")))
    (flush)
    p/exit-code))
