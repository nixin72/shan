(ns shan.parser
  (:require
   [clojure.string :as str]
   [shan.util :as u]
   [shan.config :as c]
   [shan.help :as help]
   [shan.managers :as pm]))

(defn subcmd [command config]
  (->> config
       :subcommands
       (filter #(some #{command} (vals (select-keys % [:command :short]))))
       first))

(defn command [v config]
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

      (command head config)
      (recur (first tail) (rest tail) (assoc result :command head))

      (flag head (:command result) config)
      (recur (first tail) (rest tail)
             (update result :flags conj (flag head (:command result) config)))

      (option head (:command result) config)
      (let [[head tail result] (parse-option head tail result config)]
        (recur head tail result))

      (unrecognized-option-or-flag head (:command result) config)
      (u/fatal-error "Unknown flag" (str (u/bold head) ".")
                     "Use" (u/bold "shan --help") "to see all options.")

      :else
      (recur (first tail) (rest tail) (update result :arguments conj head)))))

(defn run-cmd [arguments config]
  (let [{:keys [command flags options arguments]}
        (parse-arguments arguments config)
        continue? (atom true)]

    (when (some #{:help} flags)
      (reset! continue? false)
      (if (nil? command)
        (help/global-help config)
        (help/subcommand-help config ["shan" command])))

    (when (some #{:version} flags)
      (reset! continue? false)
      (println c/version))

    (when @continue?
      (let [run-fn (:runs (subcmd command config))]
        (if (nil? command)
          (if (first arguments)
            (u/error "Unknown command" (str (u/bold (first arguments)) ".")
                     "Use" (u/bold "shan --help") "to see all options.")
            (u/error "No command specified."))
          (run-fn (merge (reduce #(assoc %1 %2 true) {} flags)
                         {:_arguments arguments}
                         options)))))))
