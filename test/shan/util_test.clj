(ns shan.util-test
  (:require
   [clojure.test :refer [deftest testing is]]
   [flatland.ordered.map :refer [ordered-map]]
   [flatland.ordered.set :refer [ordered-set]]
   [shan.macros :refer [with-test-data suppress-side-effects suppress-stdout]]
   [shan.test-values :as v]
   [shan.util :as u]
   [shan.config :as c]))

;;;;;;;;;;; serialize ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-serialize
  (println "Testing function" (u/bold "util/serialize"))

  (testing "Test serializing an empty map"
    (is (= (u/serialize {}) {})))

  (testing "Test serializing an empty vector"
    (is (= (u/serialize []) [])))

  (testing "Test serializing an empty set"
    (is (= (u/serialize #{}) [])))

  (testing "Test serializing a map requiring no serialization"
    (is (= (u/serialize v/complex-config) v/complex-config)))

  (testing "Test serializing a map requiring serialization"
    (is (= (u/serialize v/deserialized-config) v/serialized-config)))

  (testing "Test serializing a versioned config"
    (is (= (u/serialize '{:npm {underscore "1"}})
           '{:npm {underscore "1"}})))

  (testing "Test serializing a versioned config that needs serialization"
    (is (= (u/serialize {:npm {(symbol "@react-navigation/stack") "1"}})
           '{:npm {"@react-navigation/stack" "1"}})))

  (testing "Test serializing a config with some versioned PMs and some not."
    (is (= (u/serialize '{:yay [nano] :npm {react "2"}})
           '{:yay [nano] :npm {react "2"}})))

  ;; TODO
  (testing "Test serializing a config that has PPAs"))

;;;;;;;;;;; deserialize ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-deserialize
  (println "Testing function" (u/bold "util/deserialize"))

  (testing "Test deserializing an empty map"
    (is (= (u/deserialize {}) {})))

  (testing "Test deserializing an empty vector"
    (is (= (u/deserialize []) #{})))

  (testing "Test deserializing a map requiring no changes"
    (is (= (u/deserialize v/complex-config)
           v/deserialized-complex-config)))

  (testing "Test deserializing a map requiring changes"
    (is (= (u/deserialize v/serialized-config) v/deserialized-config)))

  (testing "Test deserializing a versioned config"
    (is (= (u/deserialize '{:npm {underscore "1"}})
           '{:npm {underscore "1"}})))

  (testing "Test deserializing a versioned config that needs serialization"
    (is (= (u/deserialize {:npm {"@react-navigation/stack" "1"}})
           {:npm {(symbol "@react-navigation/stack") "1"}})))

  (testing "Test serializing a config with some versioned PMs and some not."
    (is (= (u/deserialize '{:yay [nano] :npm {react "2"}})
           '{:yay #{nano} :npm {react "2"}})))

  ;; TODO
  (testing "Test deserializing a config that has PPAs"))

;;;;;;;;;;; unordered= ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-unordered=
  (println "Testing function" (u/bold "util/unordered="))

  (testing "Test with empty unordered data structures"
    (is (true? (u/unordered= {} {})))
    (is (true? (u/unordered= #{} #{}))))

  (testing "Test with empty ordered data structures"
    (is (true? (u/unordered= [] [])))
    (is (true? (u/unordered= () ())))
    (is (true? (u/unordered= (ordered-set) (ordered-set))))
    (is (true? (u/unordered= (ordered-map) (ordered-map)))))

  (testing "Test with small non-nested unordered data"
    (is (true? (u/unordered= {:a :b :c :d} {:c :d :a :b})))
    (is (true? (u/unordered= #{:a :b :c} #{:c :a :b}))))

  (testing "Test with small non-nested ordered data"
    (is (true? (u/unordered= [:a :b :c] [:c :a :b])))
    (is (true? (u/unordered= (list :a :b :c) [:c :a :b])))
    (is (true? (u/unordered= (ordered-set :a :b :c) (ordered-set :c :a :b))))
    (is (true? (u/unordered= (ordered-map :a :b :c :d) (ordered-map :c :d :a :b)))))

  (testing "Test with small nested unordered data"
    (is (true? (u/unordered= {:a {:b :c} :d :e} {:d :e :a {:b :c}})))
    (is (true? (u/unordered= #{:a :b #{:c :d}} #{#{:d :c} :a :b}))))

  (testing "Test with small nested ordered data"
    (is (true? (u/unordered= [:a [:b :c] :d] [[:c :b] :d :a])))
    (is (true? (u/unordered= (list :a [:b :c] :d) [(ordered-set :c :b) :d :a])))
    (is (true? (u/unordered= (ordered-set :a [:b :c] :d)
                             [:a (ordered-set :b :c) :d])))
    (is (true? (u/unordered= (ordered-map :a [:b :c] :d (ordered-set :e :f))
                             (ordered-map :d (ordered-set :f :e) :a [:c :b]))))))

;;;;;;;;;;; do-merge ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-do-merge
  (println "Testing function" (u/bold "util/do-merge"))

  (testing "Test merging two empty maps"
    (is (= (u/do-merge {} {}) {})))

  (testing "Test merging with a complex map with an empty one"
    (is (= (u/do-merge v/complex-config {}) v/complex-config)))

  (testing "Test merging two identical maps"
    (is (u/unordered=
         (u/do-merge v/complex-config v/complex-config)
         v/complex-config)))

  (testing "Test merging two different configs"
    (is (u/unordered=
         (u/do-merge '{:yay [micro] :npm [expo] :pip [thefuck]}
                     '{:default-manager :yay :yay [nano] :npm [react]})
         v/complex-config))))

;;;;;;;;;;; merge-conf/1 ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-merge-conf:1
  (println "Testing function" (u/bold "util/merge-conf:1"))

  (testing "Test merging with a single empty map"
    (is (= (u/merge-conf {}) {})))

  (testing "Test merging with a single complex map"
    (is (= (u/merge-conf v/complex-config) v/complex-config))))

;;;;;;;;;;; merge-conf/2 ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-merge-conf:2
  (println "Testing function" (u/bold "util/merge-conf:2"))

  (testing "Test merging empty configs"
    (is (= (u/merge-conf {} {}) {})))

  (testing "Test merging with the first config empty"
    (is (= (u/merge-conf {} v/complex-config) v/complex-config)))

  (testing "Test merging with the second config empty"
    (is (u/unordered=
         (u/merge-conf v/complex-config {}) v/complex-config)))

  (testing "Test merging identical configs"
    (is (u/unordered=
         (u/merge-conf v/complex-config v/complex-config) v/complex-config)))

  (testing "Test merging two unique configs"
    (is (u/unordered=
         (u/merge-conf
          '{:yay [micro] :npm [expo] :pip [thefuck]}
          '{:yay [nano] :npm [react] :default-manager :yay})
         v/complex-config)))

  (testing "Test overlapping configs"
    (is (u/unordered= (u/merge-conf
                       '{:yay [micro nano] :npm [expo] :pip [thefuck]}
                       '{:yay [micro nano] :npm [react] :default-manager :yay})
                      v/complex-config))))

;;;;;;;;;;; merge-conf/3 ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-merge-conf:3
  (println "Testing function" (u/bold "util/merge-conf:3"))

  (testing "Test merging empty configs"
    (is (= (u/merge-conf {} {} {}) {})))

  (testing "Test merging with the first configs empty"
    (is (= (u/merge-conf {} {} v/complex-config) v/complex-config)))

  (testing "Test merging with the second config full"
    (is (u/unordered=
         (u/merge-conf {} v/complex-config {})
         v/complex-config)))

  (testing "Test merging with the first config full"
    (is (u/unordered=
         (u/merge-conf v/complex-config {} {})
         v/complex-config)))

  (testing "Test merging identical configs"
    (is (u/unordered=
         (u/merge-conf v/complex-config v/complex-config v/complex-config)
         v/complex-config)))

  (testing "Test merging three unique configs"
    (is (u/unordered=
         (u/merge-conf
          '{:npm [expo]}
          '{:yay [micro] :pip [thefuck]}
          '{:yay [nano] :npm [react] :default-manager :yay})
         v/complex-config)))

  (testing "Test overlapping configs"
    (is (u/unordered=
         (u/merge-conf
          '{:npm [expo react]}
          '{:yay [micro] :pip [thefuck]}
          '{:yay [nano micro] :npm [react] :default-manager :yay})
         v/complex-config))))

;;;;;;;;;;; flat-map->map ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-flat-map->map
  (println "Testing function" (u/bold "util/flat-map->map"))

  (testing "Test with empty input"
    (is (= (u/flat-map->map v/empty-input v/default-package-manager)
           {})))

  (testing "Test with a simple, single input"
    (is (u/unordered=
         (u/flat-map->map v/simple-input v/default-package-manager)
         {v/default-package-manager '[fzf]})))

  (testing "Test with a multiple packages from the default manager."
    (is (u/unordered=
         (u/flat-map->map v/multiple-input v/default-package-manager)
         {v/default-package-manager '[fzf neofetch]})))

  (testing "Test with multiple package managers"
    (is (u/unordered=
         (u/flat-map->map v/multiple-pm-input v/default-package-manager)
         {v/default-package-manager '[fzf neofetch]
          :npm '[react]})))

  (testing "Test with lots of package managers"
    (is (u/unordered=
         (u/flat-map->map v/several-pm-input v/default-package-manager)
         {v/default-package-manager '[fzf neofetch]
          :npm '[react]
          :pip '[thefuck]
          :gem '[yaml]})))

  (testing "Test with the same package manager repeated in input."
    (is (u/unordered=
         (u/flat-map->map v/same-pm-twice-input v/default-package-manager)
         {v/default-package-manager '[fzf neofetch atop]
          :npm '[react expo]}))))

;;;;;;;;;;; remove-from-config ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-remove-from-config
  (println "Testing function" (u/bold "util/remove-from-config"))

  (testing "Test with empty input"
    (is (= (u/remove-from-config {} {}) {})))

  (testing "Test with removing from empty config"
    (is (= (u/remove-from-config {} v/complex-config) {})))

  (testing "Test with removing with empty config"
    (is (= (u/remove-from-config v/complex-config {}) v/complex-config)))

  (testing "Test with removing non-overlapping config"
    (is (= (u/remove-from-config
            v/complex-config '{:yay [atop htop] :npm [react-native] :gem [yaml]})
           v/complex-config))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;; NOTE: Stateful tests beyond this point ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Everything beyond here is testing things that would normally be stateful.
;; However, everything that shan does that is truly stateful falls out of the
;; scope of these tests.
;;
;; For example, testing that writing to a file works correctly. I don't need to
;; ensure that the file is being written to. That would be a test of Clojure's
;; file writing utilities. Instead, I'm simply testing that the correct string
;; or object *would* be written to the file.
;;
;; Same goes for adding generations or installing/removing packages.
;;
;; I don't need to ensure a package is installed correctly, I need to ensure the
;; correct command to install that package is being generated.
;;
;; So these test will be testing up to the point of statefulness, however, all
;; tests will be wrapped in `suppress-side-effects'. This redefines Clojure's
;; clojure.java.shell/sh and clojure.pprint/pprint. This ensures that none of
;; the tests being run have any real side-effects on the system.

;;;;;;;;;;; read-edn ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-read-edn
  (println "Testing function" (u/bold "util/read-edn"))

  (testing "Read from non-existant file"
    (is (= (u/read-edn "path/to/garbage-file-name.edn")
           nil)))

  (testing "Read from test data config file"
    (is (= (with-test-data
             (u/read-edn c/conf-file))
           v/deserialized-complex-config)))

  (testing "Read from test data generation file"
    (is (= (with-test-data
             (u/read-edn c/gen-file))
           [v/deserialized-complex-config]))))

;;;;;;;;;;; write-edn ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-write-edn
  (println "Testing function" (u/bold "util/write-edn"))

  (testing "Write to non-existant file"
    (is (= (u/write-edn "path/to/garbage-file-name.edn" {}) nil)))

  (testing "Write empty hash to default config file"
    (is (= (suppress-side-effects (u/write-edn c/conf-file {})) {})))

  (testing "Write empty vector to generation file"
    (is (= (suppress-side-effects (u/write-edn c/gen-file [])) [])))

  (testing "Write vector requiring no serialization to generation file"
    (is (= (suppress-side-effects
            (u/write-edn c/gen-file [v/complex-config]))
           [v/complex-config])))

  (testing "Write hash that requires no serialization to default config file"
    (is (= (suppress-side-effects
            (u/write-edn c/conf-file '{:yay [atop htop]}))
           '{:yay [atop htop]})))

  (testing "Write hash that requires serialization to default config file"
    (is (= (suppress-side-effects
            (u/write-edn c/conf-file {:yay '[atop htop]
                                      :npm [(symbol "@react-navigation/stack")]}))
           '{:yay [atop htop]
             :npm ["@react-navigation/stack"]}))))

;;;;;;;;;;; get-new ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-get-new
  (println "Testing function" (u/bold "util/get-new"))

  (testing "Get test config file"
    (is (= (with-test-data (u/get-new))
           v/deserialized-complex-config))))

;;;;;;;;;;; get-temp ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-get-temp
  (println "Testing function" (u/bold "util/get-temp"))

  (testing "Get test temp file"
    (is (= (with-test-data (u/get-temp))
           v/deserialized-duplicating-config))))

;;;;;;;;;;; get-old ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-get-old
  (println "Testing function" (u/bold "util/get-old"))

  (testing "Get test temp file"
    (is (= (with-test-data (u/get-old))
           [v/deserialized-complex-config]))))

;;;;;;;;;;; add-generation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-add-generation
  (println "Testing function" (u/bold "util/add-generation"))

  (suppress-stdout
   (testing "Add a single empty map as generation."
     (is (= (with-test-data
              (u/add-generation {}))
            [v/complex-config {}])))

   (testing "Ensure the correct generations after several adds."
     (is (= (with-test-data (do (u/add-generation {})
                                (u/add-generation {})
                                (u/add-generation {})))
            [v/complex-config {} {} {}])))))

;;;;;;;;;;; add-to-temp ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-add-to-temp
  (println "Testing function" (u/bold "util/add-to-temp"))

  (testing "Add an empty map, making no changes."
    (is (= (u/deserialize (with-test-data (u/add-to-temp {})))
           (u/deserialize v/duplicating-config))))

  (testing "Add an identical map, making no changes"
    (is (= (u/deserialize (with-test-data (u/add-to-temp v/duplicating-config)))
           (u/deserialize v/duplicating-config))))

  (testing "Add a non-overlapping map."
    (is (= (u/deserialize (with-test-data (u/add-to-temp '{:gem [yaml]})))
           (u/deserialize (update v/duplicating-config :gem conj 'yaml))))))

;;;;;;;;;;; add-to-conf ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-add-to-conf
  (println "Testing function" (u/bold "util/add-to-conf"))

  (testing "Add an empty map, making no changes."
    (is (= (with-test-data (u/add-to-conf {}))
           v/complex-config)))

  (testing "Add an identical map, making no changes"
    (is (= (with-test-data (u/add-to-conf v/complex-config))
           v/complex-config)))

  (testing "Add a non-overlapping map."
    (is (= (with-test-data (u/add-to-conf '{:gem [yaml]}))
           (assoc v/complex-config :gem '[yaml])))))

;;;;;;;;;;; remove-from-temp ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-remove-from-temp
  (println "Testing function" (u/bold "util/remove-from-temp"))

  (testing "Remove an empty map, making no changes."
    (is (= (u/deserialize
            (with-test-data (u/remove-from-temp {})))
           (u/deserialize v/duplicating-config))))

  (testing "Remove an identical map, making it empty"
    (is (= (with-test-data (u/remove-from-temp v/duplicating-config))
           {})))

  (testing "Remove a non-overlapping map."
    (is (= (u/deserialize
            (with-test-data (u/remove-from-temp '{:gem [yaml]})))
           (u/deserialize v/duplicating-config))))

  (testing "Remove a whole manager from config"
    (is (= (u/deserialize
            (with-test-data (u/remove-from-temp '{:pip [thefuck]})))
           (u/deserialize (dissoc v/duplicating-config :pip)))))

  (testing "Remove some packages from a manager, but not all"
    (is (= (u/deserialize
            (with-test-data (u/remove-from-temp '{:npm [expo]})))
           (u/deserialize (assoc v/duplicating-config :npm '[react atop]))))))

;;;;;;;;;;; remove-from-conf ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-remove-from-conf
  (println "Testing function" (u/bold "util/remove-from-conf"))

  (testing "Remove an empty map, making no changes."
    (is (= (with-test-data (u/remove-from-conf {}))
           v/complex-config)))

  (testing "Remove an identical map, making it empty"
    (is (= (with-test-data (u/remove-from-conf v/complex-config))
           {})))

  (testing "Remove a non-overlapping map."
    (is (= (with-test-data (u/remove-from-conf '{:gem [yaml]}))
           v/complex-config)))

  (testing "Remove a whole manager from config"
    (is (= (with-test-data (u/remove-from-conf '{:pip [thefuck]}))
           (dissoc v/complex-config :pip))))

  (testing "Remove some packages from a manager, but not all"
    (is (= (with-test-data (u/remove-from-conf '{:npm [expo]}))
           (assoc v/complex-config :npm '[react])))))

;;;;;;;;;;; install-all ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-install-all
  (println "Testing function" (u/bold "util/install-all"))

  (testing "Install 0 packages"
    (is (= (suppress-side-effects (u/install-all [] identity identity))
           [])))

  (testing "Install a single package"
    (is (= (suppress-side-effects
            (u/install-all '[htop] identity identity))
           '[htop])))

  (testing "Install several packages"
    (is (= (suppress-side-effects
            (into #{} (u/install-all '[htop nano micro] identity identity)))
           '#{micro nano htop})))

  (testing "Install packages with failures"
    (is (= (suppress-side-effects
            (u/install-all '[htop nano micro] (constantly false) (constantly false)))
           [nil nil nil]))))

;;;;;;;;;;; install-all ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest test-remove-all
  (println "Testing function" (u/bold "util/install-all"))

  (testing "Remove 0 packages"
    (is (= (suppress-side-effects (u/remove-all [] identity identity))
           [])))

  (testing "Remove a single package"
    (is (= (suppress-side-effects
            (u/remove-all '[htop] identity identity))
           '[htop])))

  (testing "Remove several packages"
    (is (= (suppress-side-effects
            (into #{} (u/remove-all '[htop nano micro] identity identity)))
           '#{micro nano htop})))

  (testing "Remove packages with failures"
    (is (= (suppress-side-effects
            (u/remove-all '[htop nano micro] (constantly false) (constantly false)))
           [nil nil nil]))))
