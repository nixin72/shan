(ns shan.parser
  (:require
   [clojure.string :as str]
   [shan.print :as p]
   [shan.util :as u]
   [shan.config :as c]
   [shan.help :as help]
   [shan.managers :as pm]))

(defn subcmd [command config]
  (->> config
       :subcommands
       (filter #(some #{command} (vals (select-keys % [:command :short]))))
       first))

(defn command? [v config]
  (get (into #{} (mapcat #(vector (% :command) (% :short))
                         (:subcommands config)))
       v))

(defn get-flags [opts]
  (reduce #(if (= (:type %2) :with-flag)
             (assoc %1
                    (str "--" (:option %2)) (keyword (:option %2))
                    (str "-" (:short %2)) (keyword (:option %2)))
             %1)
          {} opts))

(defn flag [v command config]
  (and (str/starts-with? v "-")
       (get (into (get-flags (:global-opts config))
                  (get-flags (:opts (subcmd command config))))
            v)))

(defn get-options [opts]
  (reduce #(if (not= (:type %2) :with-flag)
             (assoc %1
                    (str "--" (:option %2)) %2
                    (str "-" (:short %2)) %2)
             %1)
          {} opts))

(defn option [v command config]
  (and (str/starts-with? v "-")
       (get (into (get-options (:global-opts config))
                  (get-options (:opts (subcmd command config))))
            v)))

(defn unrecognized-option-or-flag [v command config]
  (and (str/starts-with? v "-")
       (not (get (merge (get-flags (:global-opts config))
                        (get-flags (:opts (subcmd command config)))
                        (get-options (:global-opts config))
                        (get-options (:opts (subcmd command config)))
                        (reduce-kv
                         #(assoc
                           %1 (->> %2 name (str "--")) %3 (->> %2 name (str "-")) %3)
                         {}
                         pm/package-managers))
                 v))))

(defn parse-option [head tail result config]
  (let [opt (option head (:command result) config)
        key (keyword (:option opt))]
    (if (= (:type opt) :vector)
      (let [vals (take-while
                  #(not (or (flag % (:command result) config)
                            (option % (:command result) config)))
                  tail)
            head (drop (count vals) tail)
            tail (first tail)]

        [head tail
         (if (contains? (:options result) key)
           (update-in result [:options key] concat vals)
           (update result :options assoc key vals))])
      [(second tail) (drop 2 tail)
       (update result :options assoc key (first tail))])))

(defn parse-arguments [arguments config]
  (loop [head (first arguments)
         tail (rest arguments)
         result {:command nil :flags [] :options {} :arguments []}]
    (cond
      (nil? head) result

      (flag head (:command result) config)
      (recur (first tail) (rest tail)
             (update result :flags conj (flag head (:command result) config)))

      (option head (:command result) config)
      (let [[head tail result] (parse-option head tail result config)]
        (recur head tail result))

      (unrecognized-option-or-flag head (:command result) config)
      (p/fatal-error "Unknown flag" (str (p/bold head) ".")
                     "use" (p/bold "shan --help") "to see all options.")

      :else
      (recur (first tail) (rest tail)
             (if (nil? (:command result))
               ;; First argument goes to the command
               (assoc result :command head)
               ;; everything after is an argument to the command
               (update result :arguments conj head))))))

(defn run-cmd [arguments config]
  (let [{:keys [command flags options arguments]}
        (parse-arguments arguments config)]
    (cond
      (nil? (command? command config))
      (do (p/error "Unknown command"
                   (p/bold command) "."
                   "Use" (p/bold "shan --help") "to see all options.")
          (help/global-help config))

      (some #{:help} flags)
      (if (nil? command)
        (help/global-help config)
        (help/subcommand-help config ["shan" command]))

      (some #{:version} flags)
      (println c/version)

      (nil? command)
      (do (p/error "No command specified."
                   "Use" (p/bold "shan --help") "to see all options.")
          (help/global-help config))

      :else
      (let [run-fn (:runs (subcmd command config))]
        (if (nil? run-fn)
          (p/error "Internal error: no"
                   (p/bold ":runs")
                   "key defined for command" (p/bold command))

          (run-fn (merge (reduce #(assoc %1 %2 true) {} flags)
                         {:_arguments arguments}
                         options)))))))
