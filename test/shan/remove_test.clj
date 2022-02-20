(ns shan.remove-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [shan.print :as p]
   [shan.util :as u]
   [shan.test-values :as v]
   [shan.macros :refer [suppress-stdout with-input-queue with-test-env]]
   [shan.commands.remove :as rm]))

;;;;;;;;;;; remove-with-pm-from-list ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-remove-with-pm-from-list
  (println "Testing function" (p/bold "remove/remove-with-pm-from-list"))

  (suppress-stdout
   (testing "Test removing from first"
     (is (= (with-input-queue ["0"]
              (rm/remove-with-pm-from-list [:npm :pacman] "atop"))
            {:npm ["atop"]})))

   (testing "Test removing from second"
     (is (= (with-input-queue ["1"]
              (rm/remove-with-pm-from-list [:npm :pacman] "atop"))
            {:pacman ["atop"]})))

   (testing "Test removing from both"
     (is (= (with-input-queue ["2"]
              (rm/remove-with-pm-from-list [:npm :pacman] "atop"))
            {:pacman ["atop"] :npm ["atop"]})))

   (testing "Test removing with non-number input"
     (is (= (with-input-queue '("a" "yay" "0")
              (rm/remove-with-pm-from-list [:npm :pacman] "atop"))
            {:npm ["atop"]})))))

;;;;;;;;;;; remove-with-pm-from-installed ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-remove-with-pm-from-installed
  (println "Testing function" (p/bold "remove/remove-with-pm-from-installed"))

  (with-test-env [_ v/pre-installed-packages]
    (testing "Test with non-existant package"
      (is (= (with-input-queue ["0"]
               (rm/remove-with-pm-from-installed "unknown-package"))
             {})))

    (testing "Test with package installed in a single package manager"
      (is (= (rm/remove-with-pm-from-installed 'neovim)
             '{:pacman [neovim]})))

    (testing "Test with package installed in several package managers"
      (is (= (with-input-queue ["0"]
               (rm/remove-with-pm-from-installed 'atop))
             '{:pacman [atop]})))))

;;;;;;;;;;; find-package-manager ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-find-package-manager
  (println "Testing function" (p/bold "remove/find-package-manager"))

  (with-test-env [_ v/pre-installed-packages]
    (testing "Test finding package manager for single package"
      (is (= (rm/find-package-manager v/duplicating-config '[make])
             '{:pacman [make]})))

    (testing "Test multiple packages from the same package manager"
      ;; Make this one unordered for the vector test
      (is (= (u/serialize
              (rm/find-package-manager v/duplicating-config '[make unzip]))
             '{:pacman #{make unzip}})))

    (testing "Test multiple package from various package managers"
      (is (= (rm/find-package-manager v/duplicating-config '[json make])
             '{:gem [json] :pacman [make]})))

    (testing "Test single package in multiple package managers"
      (is (= (with-input-queue ["0"]
               (rm/find-package-manager v/duplicating-config '[readline]))
             '{:yay [readline]})))

    (testing "Test single package installed with single package managers"
      (is (= (rm/find-package-manager {} '[openssl])
             '{:gem [openssl]})))

    (testing "Test single package installed with several package managers"
      (is (= (with-in-str "0"
               (rm/find-package-manager {} '[readline]))
             '{:gem [readline]})))

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
       (mapcat keys)
       (into #{})))

;;;;;;;;;;; test-cli-remove ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-cli-remove
  (println "Testing function" (p/bold "remove/cli-remove"))

  (with-test-env [_ v/pre-installed-packages]
    (testing "Test removing a single known package"
      (is (= (config->set (rm/cli-remove {:_arguments ["micro"]}))
             #{"yay -R --noconfirm micro"})))

    (testing "Test removing several known packages from same manager"
      (is (= (config->set (rm/cli-remove {:_arguments ["micro" "nano"]}))
             #{"yay -R --noconfirm micro"
               "yay -R --noconfirm nano"})))

    (testing "Test removing several known packages from different managers"
      (is (= (config->set (rm/cli-remove {:_arguments ["micro" "expo"]}))
             #{"yay -R --noconfirm micro"
               "npm uninstall --global expo"})))

    (testing "Test removing with specified manager"
      (is (= (config->set (rm/cli-remove {:_arguments [":npm" "underscore"]}))
             #{"npm uninstall --global underscore"})))

    (testing "Test removing with several specified managers"
      (is (= (config->set (rm/cli-remove {:_arguments [":npm" "underscore" ":yay" "micro"]}))
             #{"npm uninstall --global underscore"
               "yay -R --noconfirm micro"})))

    (testing "Test removing with non-existant package"
      (is (= (config->set (rm/cli-remove {:_arguments ["some-garbage-input"]}))
             #{})))))
