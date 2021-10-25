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
       (filter #(= (:command %) command))
       first))

(defn command [v config]
  (get (into #{} (map :command (:subcommands config))) v))

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

      :else
      (recur (first tail) (rest tail) (update result :arguments conj head)))))

(defn run-cmd [arguments config]
  (let [{:keys [command flags options arguments]}
        (parse-arguments arguments config)]

    (prn command flags options arguments)

    #_(when (some #{"--help" "-h"} flags)
        (if (nil? command)
          (help/global-help config)
          (help/subcommand-help config ["shan" command])))

    #_(when (some #{"version" "v"} flags)
        (println c/version))

    ;; (prn command flags options arguments)

    #_(let [run-fn (:runs (subcmd config command))]
        (prn (merge (reduce #(assoc %1 %2 true) {} flags)
                    {:_arguments arguments}
                    options)))))
