(ns shan.managers-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [shan.macros :refer [with-test-env suppress-side-effects]]
   [shan.managers :as pm]
   [shan.packages :as ps]
   [shan.print :as p]
   [shan.test-values :as v]))

;;;;;;;;;;; installed-managers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-installed-managers
  (println "Testing function" (p/bold "managers/installed-managers"))

  (with-test-env [_ v/pre-installed-packages]
    ;; TODO: How to test that they're actually on the system?
    (testing "All managers should appear as installed"
      (is (= (pm/installed-managers)
             pm/package-managers)))))

;;;;;;;;;;; determine-default-manager ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-determine-default-manager
  (println "Testing function" (p/bold "managers/determine-default-manager")))

;;;;;;;;;;; make-fn ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-make-fn
  (println "Testing function" (p/bold "managers/make-fn"))

  (suppress-side-effects
   (testing "Test passing a function as input"
     (is (= (ps/make-fn identity false)
            identity)))

   (testing "Test passing a string as input"
     ;; I don't really know how to test this
     (is (= 0 0)))

   (testing "Test passing a string as input"
     ;; I don't really know how to test this
     (is (= 0 0)))))

;;;;;;;;;;; install-pkgs ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-install-pkgs
  (println "Testing function" (p/bold "managers/install-pkgs"))

  (suppress-side-effects
   (testing "Test installing no packages"
     (is (= (ps/install-pkgs :npm [] false)
            {})))

   (testing "Test installing a single package"
     (is (= (into #{} (keys (ps/install-pkgs :npm '[underscore] false)))
            #{"npm install --global underscore"})))

   (testing "Test installing several packages"
     (is (= (into #{} (keys (ps/install-pkgs :npm '[react expo underscore] true)))
            #{"npm install --global react"
              "npm install --global expo"
              "npm install --global underscore"})))))

;;;;;;;;;;; remove-pkgs ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-remove-pkgs
  (println "Testing function" (p/bold "managers/remove-pkgs"))

  (suppress-side-effects
   (testing "Test removing no packages"
     (is (= (ps/remove-pkgs :npm [] false)
            {})))

   (testing "Test removing a single package"
     (is (= (into #{} (keys (ps/remove-pkgs :npm '[underscore] false)))
            #{"npm uninstall --global underscore"})))

   (testing "Test removing several packages"
     (is (= (into #{} (keys (ps/remove-pkgs :npm '[react expo underscore] true)))
            #{"npm uninstall --global react"
              "npm uninstall --global expo"
              "npm uninstall --global underscore"})))))
