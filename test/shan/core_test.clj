(ns shan.core-test
  (:require
   [clojure.test :refer [run-all-tests]]
   [shan.print :as p]
   [shan.util-test]
   [shan.install-test]
   [shan.remove-test]
   [shan.managers-test]
   [shan.config :as c]
   [shan.test-values :as tv]))

(defn -main [& [verbose?]]
  (binding [tv/verbose? (some #{"-v" "--verbose"} [verbose?])
            c/testing? true]
    (let [{:keys [fail error]}
          ;; util|install|remove|managers|parser
          (run-all-tests #"shan\.(util|install|remove|managers|parser)-test")]
      (if (= 0 fail error)
        (println (p/green "Success:") "all test cases passed.")
        (do (println (p/green "Failure:") "you have" (+ fail error) "failing test cases.")
            (System/exit 1)))))

  (System/exit 0))
