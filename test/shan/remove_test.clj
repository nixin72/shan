(ns shan.remove-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [shan.util :as u]
   [shan.test-values :as v]
   [shan.macros :refer [suppress-stdout with-test-data]]
   [shan.remove :as rm]))

;;;;;;;;;;; remove-with-pm-from-list ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-remove-with-pm-from-list
  (println "Testing function" (u/bold "remove/remove-with-pm-from-list"))

  (suppress-stdout
   (testing "Test removing from first"
     (is (= (with-in-str "0"
              (rm/remove-with-pm-from-list [:npm :yay] "atop"))
            {:npm ["atop"]})))

   (testing "Test removing from second"
     (is (= (with-in-str "1"
              (rm/remove-with-pm-from-list [:npm :yay] "atop"))
            {:yay ["atop"]})))

   (testing "Test removing from both"
     (is (= (with-in-str "2"
              (rm/remove-with-pm-from-list [:npm :yay] "atop"))
            {:yay ["atop"] :npm ["atop"]})))

   (testing "Test removing with non-number input"
     (is (thrown? java.lang.StackOverflowError
                  (with-in-str "a"
                    (rm/remove-with-pm-from-list [:npm :yay] "atop")))))))

;;;;;;;;;;; remove-with-pm-from-installed ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-remove-with-pm-from-installed
  (println "Testing function" (u/bold "remove/remove-with-pm-from-installed"))

  (suppress-stdout
   (testing "Test with non-existant package"
     (is (= (rm/remove-with-pm-from-installed "unknown-package") {})))

   (testing "Test with package installed in a single package manager"
     (is (= (with-test-data
              (rm/remove-with-pm-from-installed "underscore"))
            {:npm ["underscore"]})))

   (testing "Test with package installed in several package managers"
     (is (= (with-in-str "0"
              (rm/remove-with-pm-from-installed "atop"))
            {:yay ["atop"]})))))

;;;;;;;;;;; find-package-manager ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-find-package-manager
  (println "Testing function" (u/bold "remove/find-package-manager"))

  (suppress-stdout
   (testing "Test finding package manager for single package"
     (is (= (rm/find-package-manager v/duplicating-config '[htop])
            '{:yay [htop]})))

   (testing "Test multiple packages from the same package manager"
     (is (= (rm/find-package-manager v/duplicating-config '[nano micro])
            '{:yay [micro nano]})))

   (testing "Test multiple package from various package managers"
     (is (= (rm/find-package-manager v/duplicating-config '[nano react])
            '{:npm [react] :yay [nano]})))

   (testing "Test single package in multiple package managers"
     (is (= (with-in-str "0"
              (rm/find-package-manager v/duplicating-config '[atop]))
            '{:yay [atop]})))

   (testing "Test single package installed with single package managers"
     (is (= (with-test-data
              (rm/find-package-manager {} '[underscore]))
            '{:npm [underscore]})))

   (testing "Test single package installed with several package managers"
     (is (= (with-in-str "0"
              (rm/find-package-manager {} '[atop]))
            '{:yay [atop]})))

   (testing "Test single package installed with no package managers"
     (is (= (rm/find-package-manager {} '[package-does-not-exist])
            '{})))))

;; NOTE: The following tests are working with stateful code, however they do not
;; test the side effects of the changes of state. This code deals with removing
;; packages from the system, however it doesn't ensure the package was
;; successfully deleted or not. Instead, it ensures that the correct commands
;; are generated to successfully remove those packages. If those packages are
;; not successfully removed, then as long as the command is successfully
;; generated it's probably not a problem with shan.

(defn config->set [config]
  (->> (vals config)
       (map keys)
       (apply concat)
       (into #{})))

;;;;;;;;;;; test-cli-remove ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-cli-remove
  (println "Testing function" (u/bold "remove/cli-remove"))

  (suppress-stdout
   (testing "Test removing a single known package"
     (is (= (with-test-data
              (config->set (rm/cli-remove {:_arguments ["micro"]})))
            #{"yay -R --noconfirm micro"})))

   (testing "Test removing several known packages from same manager"
     (is (= (with-test-data
              (config->set (rm/cli-remove {:_arguments ["micro" "nano"]})))
            #{"yay -R --noconfirm micro"
              "yay -R --noconfirm nano"})))

   (testing "Test removing several known packages from different managers"
     (is (= (with-test-data
              (config->set (rm/cli-remove {:_arguments ["micro" "expo"]})))
            #{"yay -R --noconfirm micro"
              "npm uninstall --global expo"})))

   (testing "Test removing with specified manager"
     (is (= (with-test-data
              (config->set (rm/cli-remove {:_arguments [":npm" "underscore"]})))
            #{"npm uninstall --global underscore"})))

   (testing "Test removing with several specified managers"
     (is (= (with-test-data
              (config->set (rm/cli-remove {:_arguments [":npm" "underscore" ":yay" "micro"]})))
            #{"npm uninstall --global underscore"
              "yay -R --noconfirm micro"})))

   (testing "Test removing with non-existant package"
     (is (= (with-test-data
              (config->set (rm/cli-remove {:_arguments ["some-garbage-input"]})))
            #{})))))
