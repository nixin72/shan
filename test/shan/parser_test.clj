(ns shan.parser-test
  (:require
   [shan.parser :as p]
   [clojure.test :refer [deftest testing is]]
   [shan.print :as pr]
   [shan.core :as core]
   [shan.edit :as edit]))

;;;;;;;;;;; test-subcmd ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-subcmd
  (println "Testing function" (pr/bold "parser/subcmd"))

  (testing "Testing simple subcommand"
    (is (= (p/subcmd "merge" core/config)
           {:command "merge"
            :short "mg"
            :category "Managing Packages"
            :arguments? 0
            :description "Merges all temporary packages into your config file"
            :runs edit/cli-merge})))

  (testing "Testing invalid subcommand"
    (is (= (p/subcmd "npm" core/config)
           nil))))

;;;;;;;;;;; test-command ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-command
  (println "Testing function" (pr/bold "parser/command"))

  (testing "Testing simple subcommand"
    (is (= (p/command? "merge" core/config) "merge")))

  (testing "Testing invalid subcommand"
    (is (= (p/command? "npm" core/config) nil))))

;;;;;;;;;;; test-get-flags ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-get-flags
  (println "Testing function" (pr/bold "parser/get-flags"))

  (testing "Testing global flags"
    (is (= (p/get-flags (:global-opts core/config))
           {"--help" :help "-h" :help
            "--version" :version "-v" :version})))

  (testing "Testing local flags"
    (is (= (p/get-flags (-> core/config :subcommands second :opts))
           {"--verbose" :verbose "-v" :verbose
            "--temp" :temp "-t" :temp})))

  (testing "Testing no options at all"
    (is (= (p/get-flags (-> core/config :subcommands first :opts))
           {})))

  (testing "Testing no flag options specifically"
    (is (= (p/get-flags (->> core/config :subcommands
                             (filter #(= (:command %) "list"))
                             first :opts))
           {"--temp" :temp "-t" :temp}))))

;;;;;;;;;;; test-flags ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-flag
  (println "Testing function" (pr/bold "parser/flag"))

  (testing "Test getting a global flag"
    (is (= (p/flag "--help" nil core/config)
           :help)))

  (testing "Testing getting a local flag"
    (is (= (p/flag "--temp" "install" core/config)
           :temp)))

  (testing "Test getting an overridden glad"
    (is (= (p/flag "-v" nil core/config) :version))
    (is (= (p/flag "-v" "install" core/config) :verbose))))

;;;;;;;;;;; test-get-options ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-get-options
  (println "Testing function" (pr/bold "parser/get-options"))

  (testing "Testing global options"
    (is (= (p/get-options (:global-opts core/config)) {})))

  (testing "Testing options specifically"
    (is (= (p/get-options (->> core/config :subcommands
                               (filter #(= (:command %) "list"))
                               first :opts))
           (let [opt-config (->> core/config :subcommands
                                 (filter #(= (:command %) "list"))
                                 first :opts
                                 (filter #(= (:option %) "format"))
                                 first)]
             {"--format" opt-config, "-f" opt-config}))))

  (testing "Testing no options at all"
    (is (= (p/get-options (-> core/config :subcommands first :opts)) {}))))

;;;;;;;;;;; test-option ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-option
  (println "Testing function" (pr/bold "parser/option"))

  (testing "Test getting a string option"
    (is (= (p/option "--format" "list" core/config)
           (->> (p/subcmd "list" core/config)
                :opts
                (filter #(= (:option %) "format"))
                first)))))

;;;;;;;;;;; test-parse-option ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-parse-option
  (println "Testing function" (pr/bold "parser/parse-option"))

  (testing "Test parsing a simple option"
    (is (= (p/parse-option "--format" ["json" "--temp"] {:command "list" :options {}} core/config)
           ["--temp" () {:command "list" :options {:format "json"}}]))))

;;;;;;;;;;; test-parse-arguments ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-parse-arguments
  (println "Testing function" (pr/bold "parser/parse-arguments"))

  (testing "Test parsing empty input"
    (is (= (p/parse-arguments [] core/config)
           {:command nil :flags [] :options {} :arguments []})))

  (testing "Test parsing single command with no flags/options/arguments"
    (is (= (p/parse-arguments ["list"] core/config)
           {:command "list" :flags [] :options {} :arguments []})))

  (testing "Test parsing command with one long flags"
    (is (= (p/parse-arguments ["list" "--temp"] core/config)
           {:command "list" :flags [:temp] :options {} :arguments []})))

  (testing "Test parsing command with one short flags"
    (is (= (p/parse-arguments ["list" "-t"] core/config)
           {:command "list" :flags [:temp] :options {} :arguments []})))

  (testing "Test parsing command with multiple flags"
    (is (= (p/parse-arguments ["install" "-t" "--verbose"] core/config)
           {:command "install"
            :flags [:temp :verbose]
            :options {} :arguments []})))

  (testing "Test parsing command with single long option"
    (is (= (p/parse-arguments ["list" "--format" "json"] core/config)
           {:command "list" :flags [] :options {:format "json"} :arguments []})))

  (testing "Test parsing command with combination of arguments and flags"
    (is (= (p/parse-arguments ["list" "-t" "-f" "json"] core/config)
           {:command "list"
            :flags [:temp]
            :options {:format "json"}
            :arguments []})))

  (testing "Test parsing command with flags and arguments"
    (is (= (p/parse-arguments ["install" "-t" "npm" "--npm" "react"] core/config)
           {:command "install"
            :flags [:temp]
            :options {}
            :arguments ["npm" "--npm" "react"]}))))
