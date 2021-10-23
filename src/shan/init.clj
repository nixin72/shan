(ns shan.init
  (:require
   [shan.config :as c]
   [shan.managers :as pm]
   [shan.util :as u]
   [shan.edit :as edit]))

(defn cli-init [& args]
  (u/warning "Do NOT include versions in your generation. It WILL break things.")
  (let [config
        (->> (pm/installed-managers)
             (reduce-kv (fn [a k v] (conj a (into v {:name k}))) [])
             (group-by :type)
             (vals)
             (apply concat)
             (reduce
              (fn [a v]
                (if (contains? v :uses)
                  a
                  (if (u/yes-or-no
                       true
                       "Would you like to include" (-> v :name name u/bold))
                    (assoc a (:name v) ((:list v)))
                    a)))
              {}))
        existing-config? (boolean (seq (u/get-new)))]
    (if existing-config?
      (when (u/yes-or-no false "You config file is not empty. Would you like to overwrite it")
        (u/write-edn c/conf-file config))
      (u/write-edn c/conf-file config))
    (when (u/yes-or-no true
                       "Would you like to edit your config"
                       "(Use this opportunity to remove what you don't need)")
      (edit/cli-edit nil))
    (if c/testing? config u/exit-code)))
