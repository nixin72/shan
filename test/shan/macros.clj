(ns shan.macros
  (:require
   [clojure.java.shell]
   [clojure.java.io :as io]
   [shan.config :as c]
   [shan.test-values :as tv]))

(defn make-test-files []
  (-> c/conf-dir java.io.File. .mkdirs)
  (-> c/gen-dir java.io.File. .mkdirs)
  (-> c/conf-file java.io.File. .createNewFile)
  (-> c/temp-file java.io.File. .createNewFile)
  (-> c/gen-file java.io.File. .createNewFile)

  (spit c/conf-file (str tv/complex-config))
  (spit c/temp-file (str tv/duplicating-config))
  (spit c/gen-file (str [tv/complex-config])))

(defmacro suppress-stdout [& body]
  `(binding [*out* *out*]
     (set! *out* (io/writer "/dev/null" :append true))
     (do ~@body)))

(defmacro suppress-side-effects [& body]
  `(with-redefs [clojure.java.shell/sh (fn [& xs#] xs#)
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
     (binding [shan.config/conf-dir (str shan.config/home "/" shan.config/config)
               shan.config/gen-dir (str shan.config/home "/" shan.config/local)]
       (binding [shan.config/conf-file (str shan.config/conf-dir "shan.edn")
                 shan.config/temp-file (str shan.config/gen-dir "temporary.edn")
                 shan.config/gen-file (str shan.config/gen-dir "generations.edn")]

         (let [home# (java.io.File. c/home)]
           (when (.exists home#)
             (-> c/home java.io.File. .delete))
           (make-test-files)
           ~@body)))))
