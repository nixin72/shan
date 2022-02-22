(ns build
  (:require
   [clojure.tools.build.api :as b]
   [org.corfield.build :as bb]))

(def lib 'nixin72/shan)
(def class-dir "target/classes")
(def version "0.0.1")

(defn uber [_]
  (bb/uber {:lib lib :main 'shan.core})
  )

(comment
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
)
