(ns build
  (:require
   [clojure.tools.build.api :as b]))

(def class-dir "target/classes")

(def uber-basis
  (b/create-basis {:project "deps.edn"
                   :aliases [:native-deps]}))

(defn uber [_]
  (println "Compiling sources sources.")
  (b/compile-clj {:basis uber-basis
                  :src-dirs ["src"]
                  :class-dir class-dir
                  :ns-compile '[shan.core]})
  (println "Building uberjar.")
  (b/uber {:class-dir class-dir
           :uber-file "shan.jar"
           :basis uber-basis
           :main 'shan.core}))
