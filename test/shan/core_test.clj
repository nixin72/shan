(ns shan.core-test
  (:require
   [clojure.test :refer [run-all-tests]]
   [shan.util :as u]
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
          (run-all-tests #"shan\.(remove)-test")] ;util|install|remove|managers
      (if (= 0 fail error)
        (println (u/green "Success:") "all test cases passed.")
        (do (println (u/green "Failure:") "you have" (+ fail error) "failing test cases.")
            (System/exit 1)))))

  (System/exit 0))
