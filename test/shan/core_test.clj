(ns shan.core-test
  (:require
   [clojure.test :refer [run-all-tests]]
   [shan.util-test]
   [shan.install-test]
   [shan.remove-test]))

(defn -main [& _]
  (run-all-tests #"shan\.(util|install|remove)-test"))
