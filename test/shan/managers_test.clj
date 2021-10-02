(ns shan.managers-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [shan.macros :refer [suppress-side-effects suppress-state-changes]]
   [shan.managers :as pm]
   [shan.util :as u]
   [shan.test-values :as v]))

;;;;;;;;;;; make-fn ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-make-fn
  (println "Testing function" (u/bold "managers/make-fn"))

  (suppress-side-effects
   (testing "Test passing a function as input"
     (is (= (pm/make-fn identity false)
            identity)))

   (testing "Test passing a string as input"
     ;; I don't really know how to test this
     (is (= 0 0)))

   (testing "Test passing a string as input"
     ;; I don't really know how to test this
     (is (= 0 0)))))

;;;;;;;;;;; install-pkgs ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-install-pkgs
  (println "Testing function" (u/bold "managers/install-pkgs"))

  (suppress-side-effects
   (testing "Test installing no packages"
     (is (= (pm/install-pkgs :npm [] false)
            {})))

   (testing "Test installing a single package"
     (is (= (into #{} (keys (pm/install-pkgs :npm '[underscore] false)))
            #{"npm install --global underscore"})))

   (testing "Test installing several packages"
     (is (= (into #{} (keys (pm/install-pkgs :npm '[react expo underscore] true)))
            #{"npm install --global react"
              "npm install --global expo"
              "npm install --global underscore"})))))

;;;;;;;;;;; remove-pkgs ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-remove-pkgs
  (println "Testing function" (u/bold "managers/remove-pkgs"))

  (suppress-side-effects
   (testing "Test removing no packages"
     (is (= (pm/remove-pkgs :npm [] false)
            {})))

   (testing "Test removing a single package"
     (is (= (into #{} (keys (pm/remove-pkgs :npm '[underscore] false)))
            #{"npm uninstall --global underscore"})))

   (testing "Test removing several packages"
     (is (= (into #{} (keys (pm/remove-pkgs :npm '[react expo underscore] true)))
            #{"npm uninstall --global react"
              "npm uninstall --global expo"
              "npm uninstall --global underscore"})))))
