(ns shan.install-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [shan.macros :refer [suppress-stdout with-test-data]]
   [shan.util :as u]
   [shan.install :as in]))

;;;;;;;;;;; test-generate-success-report ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest test-generate-test-report
  (println "Testing function" (u/bold "install/generate-test-report"))

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

  (suppress-stdout
   (testing "Test installing a single known package"
     (is (= (with-test-data
              (in/cli-install {:_arguments ["micro"]}))
            #{"yay -S --noconfirm micro"})))

   (testing "Test installing several known packages from same manager"
     (is (= (with-test-data
              (in/cli-install {:_arguments ["micro" "nano"]}))
            #{"yay -S --noconfirm micro"
              "yay -S --noconfirm nano"})))

   (testing "Test installing several known packages from different managers"
     (is (= (with-test-data
              (in/cli-install {:_arguments ["micro" "expo"]}))
            #{"yay -S --noconfirm micro"
              "yay -S --noconfirm expo"})))

   (testing "Test installing with specified manager"
     (is (= (with-test-data
              (in/cli-install {:_arguments [":npm" "underscore"]}))
            #{"npm install --global underscore"})))

   (testing "Test installing with several specified managers"
     (is (= (with-test-data
              (in/cli-install {:_arguments [":npm" "underscore" ":yay" "micro"]}))
            #{"npm install --global underscore"
              "yay -S --noconfirm micro"})))

   (testing "Test installing with non-existant package"
     (is (= (with-test-data
              (in/cli-install {:_arguments ["some-garbage-input"]}))
            #{"yay -S --noconfirm some-garbage-input"})))))
