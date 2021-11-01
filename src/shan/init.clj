(ns shan.init
  (:require
   [shan.config :as c]
   [shan.managers :as pm]
   [shan.util :as u]
   [shan.edit :as edit]
   [clojure.string :as str]))

(defn write-config [config]
  (u/write-edn c/conf-file config)
  (when (u/yes-or-no true
                     "Would you like to edit your config"
                     "(Use this opportunity to remove what you don't need)")
    (edit/cli-edit nil)))

(defn get-valid-package-managers [arguments]
  (reduce (fn [a v]
            (if (contains? (pm/installed-managers) (keyword v))
              (conj a (keyword v))
              (u/fatal-error "No such package manager" (u/bold v))))
          #{} arguments))

(defn include-from-prompt [included manager]
  (cond
    (contains? included :uses) manager

    (u/yes-or-no true "Would you like to include" (-> manager :name name u/bold))
    (assoc included (:name manager) ((:list manager)))

    :else included))

(defn include-from-set [included manager set]
  (cond
    (contains? manager :uses) included
    (contains? set (:name manager))
    (do
      (println "Including packages for" (str/join ", " set))
      (assoc included (:name manager) ((:list manager))))
    :else included))

(defn include-package-managers [pms]
  (->> (pm/installed-managers)
       (reduce-kv (fn [a k v] (conj a (into v {:name k}))) [])
       (group-by :type)
       (vals)
       (apply concat)
       (reduce
        (fn [a v]
          (if (empty? pms)
            (include-from-prompt a v)
            (include-from-set a v pms)))
        {})))

(defn cli-init [{:keys [_arguments]}]
  (u/warning "Do NOT include versions in your generation. It WILL break things.")
  (let [pms (get-valid-package-managers _arguments)
        config (include-package-managers pms)
        existing-config? (boolean (seq (u/get-new)))]
    (if existing-config?
      (when (u/yes-or-no false "You config file is not empty. Would you like to overwrite it")
        (write-config config))
      (write-config config))

    (if c/testing? config u/exit-code)))
