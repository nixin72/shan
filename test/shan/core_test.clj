(ns shan.core-test
  (:require
   [clojure.java.shell :refer [sh]]
   [clojure.test :refer [run-all-tests]]
   [shan.util :as u]
   [shan.util-test]
   [shan.install-test]
   [shan.remove-test]
   [shan.managers-test]))

(defn -main [& _]
  (sh "npm" "install" "--global" "underscore")

  (let [{:keys [fail error]}
        (run-all-tests #"shan\.(util|install|remove|managers)-test")]
    (if (= 0 fail error)
      (println (u/green "Success:") "all test cases passed.")
      (do (println (u/green "Failure:") "you have" (+ fail error) "failing test cases.")
          (System/exit 1))))

  (System/exit 0))
