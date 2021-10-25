(ns shan.macros
  (:require
   [clojure.java.shell]
   [clojure.java.io :as io]
   [shan.config :as c]
   [clojure.pprint]
   [shan.util]
   [shan.managers.npm]
   [shan.test-values :as tv]
   [clojure.string :as str]
   [shan.managers :as pm]
   [shan.util :as u]))

(defn make-test-files []
  (-> c/data-dir java.io.File. .mkdirs)
  (-> c/conf-file java.io.File. .createNewFile)
  (-> c/temp-file java.io.File. .createNewFile)
  (-> c/gen-file java.io.File. .createNewFile)

  (spit c/conf-file (str tv/complex-config))
  (spit c/temp-file (str tv/duplicating-config))
  (spit c/gen-file (str [tv/complex-config])))

(defmacro suppress-stdout [& body]
  `(if tv/verbose?
     (do ~@body)
     (with-out-str ~@body)))

(defmacro with-test-env
  "Given an environment representing the pre-existing packages on the system,
  overload any values that would mutate the system and replace them with
  functions that operate on the passed-in environment instead."
  [[env-name env] & body]
  `(binding [shan.config/testing? true]
     (let [~env-name (atom ~env)]
       ;; Redefine any function that would mutate environment or state
       ;; and instead act against the env passed in.
       (with-redefs
        [clojure.java.shell/sh (fn [& xs#] {:exit 0 :out xs#})
         clojure.pprint/pprint (fn [& xs#] xs#)
         shan.managers.npm/installed? (fn [p#] (pm/make-fn "npm list --global"))
         shan.util/get-new (fn [] tv/complex-config)
         shan.util/get-temp (fn [] tv/temporary-packages)
         shan.util/get-old (fn [] [tv/complex-config])
         shan.util/already-installed?
         (fn [installed-fn# package#]
           (let [pm# (-> (installed-fn# package#) first keyword)
                 pm# (or (-> pm# pm/package-managers :uses) pm#)]
             (contains? (get @~env-name pm#) package#)))
         shan.util/add-archive
         (fn [add-fn# archive#]
           (let [pm# (-> (add-fn# archive#) first keyword)
                 pm# (or (-> pm# pm/package-managers :uses) pm#)]
             (swap! ~env-name update-in [:archives pm#] conj archive#)))
         shan.util/install-package
         (fn [install-fn# package#]
           (let [pm# (-> (install-fn# package#) first keyword)
                 pm# (or (-> pm# pm/package-managers :uses) pm#)]
             (u/identity-prn (swap! ~env-name update pm# conj package#))))
         shan.util/remove-package
         (fn [remove-fn# package#]
           (let [pm# (-> (remove-fn# package#) first keyword)
                 pm# (or (-> pm# pm/package-managers :uses) pm#)]
             (swap! ~env-name update pm# disj package#)))]
         (suppress-stdout ~@body)))))

(defmacro with-failed-operation [& body]
  `(with-redefs [shan.util/install-package (constantly false)]
     ~@body))

(defmacro with-input-queue [queue & body]
  `(let [input# (atom ~queue)]
     (with-redefs [clojure.core/read-line
                   #(let [out# (peek @input#)]
                      (swap! input# pop)
                      out#)]
       ~@body)))

(defmacro suppress-side-effects [& body]
  `(with-redefs [clojure.java.shell/sh (fn [& xs#] {:exit 0 :out xs#})
                 clojure.pprint/pprint (fn [& xs#] xs#)]
     (binding [*out* *out*]
       (set! *out* (io/writer "/dev/null" :append true))
       ~@body)))

(defmacro suppress-state-changes [& body]
  `(with-redefs [clojure.java.shell/sh (fn [& xs#] {:exit 0 :out xs#})
                 clojure.pprint/pprint (fn [& xs#] xs#)]
     ~@body))

(defmacro with-test-data [& body]
  `(binding [shan.config/home (str (System/getProperty "user.dir") "/.test")]
     (binding [shan.config/data-dir (str shan.config/home "/" shan.config/local)]
       (binding [shan.config/conf-file (str shan.config/data-dir "shan.edn")
                 shan.config/temp-file (str shan.config/data-dir "temporary.edn")
                 shan.config/gen-file (str shan.config/data-dir "generations.edn")]

         (let [home# (java.io.File. c/home)]
           (when (.exists home#)
             (-> c/home java.io.File. .delete))
           (make-test-files)
           ~@body)))))
