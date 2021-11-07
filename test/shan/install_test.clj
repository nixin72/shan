(ns shan.install-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [shan.macros :refer [suppress-stdout
                        with-test-env
                        with-failed-operation
                        with-input-queue]]
   [shan.util :as u]
   [shan.install :as in]
   [shan.test-values :as tv]))

;;;;;;;;;;; test-generate-success-report ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-generate-success-report
  (println "Testing function" (u/bold "install/generate-success-report"))

  (suppress-stdout
   (testing "Test empty results"
     (is (= (in/generate-success-report {})
            {:failed false
             :success {}
             :commands #{}})))

   (testing "Test single failing package"
     (is (= (in/generate-success-report {:yay {"yay -S --noconfirm nano" nil}})
            {:failed true
             :success {:yay []}
             :commands #{"yay -S --noconfirm nano"}})))

   (testing "Test single successful package"
     (is (= (in/generate-success-report '{:yay {"yay -S --noconfirm nano" nano}})
            {:failed false
             :success '{:yay [nano]}
             :commands #{"yay -S --noconfirm nano"}})))

   (testing "Test a successful and a failed package"
     (is (= (in/generate-success-report '{:yay {"yay -S --noconfirm nano" nano}
                                          :npm {"npm install --global expo" nil}})
            {:failed true
             :success '{:yay [nano] :npm []}
             :commands #{"yay -S --noconfirm nano"
                         "npm install --global expo"}})))

   (testing "Test a successful and a complex report"
     (is (= (in/generate-success-report '{:yay {"yay -S --noconfirm nano" nano
                                                "yay -S --noconfirm htop" htop}
                                          :pip {"python -m pip install wakatime" nil}
                                          :npm {"npm install --global expo" nil
                                                "npm install --global react" react}})
            {:failed true
             :success '{:yay [nano htop] :npm [react] :pip []}
             :commands #{"yay -S --noconfirm nano"
                         "yay -S --noconfirm htop"
                         "python -m pip install wakatime"
                         "npm install --global expo"
                         "npm install --global react"}})))))

;;;;;;;;;;; test-find-default-manager ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-find-default-manager
  (println "Testing function" (u/bold "install/find-default-manager"))

  (suppress-stdout
   (testing "Getting finding default package manager without setting it."
     (is (= (with-input-queue '("0" "n")
              (in/find-default-manager tv/install-map-simple-input))
            (u/serialize '{:paru [fzf]}))))

   (testing "Getting finding default package manager without setting it."
     (is (= (with-input-queue '("1" "y")
              (in/find-default-manager tv/install-map-simple-input))
            (u/serialize '{:yay [fzf] :default-manager :yay}))))))

;; NOTE: The following tests are working with stateful code, however they do not
;; test the side effects of the changes of state. This code deals with removing
;; packages from the system, however it doesn't ensure the package was
;; successfully deleted or not. Instead, it ensures that the correct commands
;; are generated to successfully remove those packages. If those packages are
;; not successfully removed, then as long as the command is successfully
;; generated it's probably not a problem with shan.

;;;;;;;;;;; test-cli-install ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-cli-install
  (println "Testing function" (u/bold "install/cli-install"))

  (with-test-env [env tv/pre-installed-packages]
    (testing "Test installing a single known package"
      (is (= (in/cli-install {:_arguments ["micro"]})
             #{"yay -S --noconfirm micro"})))

    (testing "Test installing several packages from same manager"
      (is (= (in/cli-install {:_arguments ["micro" "nano"]})
             #{"yay -S --noconfirm micro"
               "yay -S --noconfirm nano"})))

    (testing "Test installing with specified manager"
      (is (= (in/cli-install {:_arguments [":npm" "underscore" "react"]})
             #{"npm install --global underscore"
               "npm install --global react"})))

    (testing "Test installing with several specified managers"
      (is (= (in/cli-install {:_arguments [":npm" "underscore" ":yay" "micro"]})
             #{"npm install --global underscore"
               "yay -S --noconfirm micro"})))

    (testing "Test installing with non-existant package"
      (is (= (with-failed-operation
               (in/cli-install {:_arguments ["some-garbage-input"]}))
             #{"yay -S --noconfirm some-garbage-input"})))

    (testing "All operations completeled successfully"
      (is (= @env (-> tv/pre-installed-packages
                      (update :pacman conj 'micro 'nano)
                      (update :npm conj 'underscore 'react)))))))
