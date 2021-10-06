(ns shan.sync-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [shan.macros :refer [with-test-data suppress-state-changes]]
   [shan.sync :as s]
   [shan.util :as u]
   [shan.test-values :as v]))

(deftest test-cli-sync
  (println "Testing function" (u/bold "install/generate-success-report"))

  (suppress-state-changes
   (testing "Test with idential configs"
     (is (= (with-test-data (s/cli-sync {}))
            [{} {}])))

   (testing "Test with config to add"
     (is (= (with-test-data (s/cli-sync {}))
            [{} {}])))))
