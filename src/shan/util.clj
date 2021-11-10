(ns shan.util
  (:require
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [clojure.pprint :refer [pprint]]
   [clojure.data :as data]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.walk :as walk]
   [flatland.ordered.map :as omap :refer [ordered-map]]
   [flatland.ordered.set :as oset :refer [ordered-set]]
   [shan.config :as c]))

(def ^:dynamic *mout* *out*)
(def exit-code (atom 0))
(def normal "\033[0m")
(defn red [string]    (str "\033[0;31m" string normal))
(defn green [string]  (str "\033[0;32m" string normal))
(defn yellow [string] (str "\033[0;33m" string normal))
(defn blue [string]   (str "\033[0;34m" string normal))
(defn purple [string] (str "\033[0;35m" string normal))
(defn grey [string]   (str "\033[0;37m" string normal))
(defn bold [string]   (str "\033[0;1m"  string normal))

(defn identity-prn [arg]
  (prn arg)
  arg)

(defn identity-pprint [arg]
  (pprint arg)
  arg)

;; ensure that errors get printed, even if verbose is turned off
(defn log [& arg]
  (binding [*out* *mout*]
    (let [time (.format (java.text.SimpleDateFormat. "hh:mm:ss") (java.util.Date.))]
      (println (-> (str "[" time "]") green bold) (str/join " " arg)))))

(defn warning [& arg]
  (binding [*out* *mout*]
    (println (-> "[Warning]" yellow bold)
             (str/join " " arg))))

(defn error [& arg]
  (binding [*out* *err*]
    (reset! exit-code 1)
    (println (-> "[Error]" red bold)
             (str/join " " arg))))

(defn fatal-error
  "Prints an error and quits."
  [& args]
  (apply error args)
  (System/exit @exit-code))

(defn deserialize [ds]
  (walk/prewalk #(cond
                   (map? %) (into (ordered-map) %)
                   ((some-fn vector? list? set? %)) (into (ordered-set) %)
                   (symbol? %) (str %)
                   :else %)
                ds))

(defn serialize [ds]
  (walk/prewalk #(cond
                   (instance? flatland.ordered.map.OrderedMap %) (into {} %)
                   (instance? flatland.ordered.set.OrderedSet %) (into [] %)
                   (map? %) (into {} %)
                   ((some-fn vector? list? set?) %) (into [] %)
                   :else %)
                ds))

(defn unordered=
  "Test if two data structures are equivalent in that they contain all the
  same elements, but the elements aren't necessarily in the same order."
  [a b]
  (= (serialize a) (serialize b)))

(defn do-merge [conf1 conf2]
  (reduce-kv (fn [a k v]
               (if (contains? a k)
                 (if (keyword? v)
                   (assoc a k v)
                   (update a k #(into (ordered-set) (into % v))))
                 (assoc a k v)))
             conf1
             conf2))

(defn merge-conf
  ([] (ordered-map))
  ([conf] conf)
  ([conf1 conf2]
   (let [[old new same] (data/diff conf1 conf2)]
     (cond
       (and (nil? old) (nil? new)) same
       (nil? old) new
       (nil? new) old
       (seq same) (merge-conf (do-merge old new) same)
       :else (do-merge old new))))
  ([conf1 conf2 & confs]
   (apply merge-conf (merge-conf conf1 conf2) confs)))

(defn flat-map->map [vector-map default-key]
  (->> (reduce (fn [a v]
                 (let [kw (keyword (subs v 1))]
                   (cond
                     (= v (str kw)) (into a [kw []])
                     (= a []) (into a [default-key [v]])
                     :else (conj (pop a) (conj (last a) v)))))
               []
               vector-map)
       (partition 2)
       (sort-by first)
       (reduce (fn [a [k v]]
                 (if (contains? a k)
                   (update a k into v)
                   (assoc a k (into (ordered-set) v))))
               (ordered-map))))

(defn remove-from-config [conf pkgs]
  (reduce-kv
   (fn [a k v]
     (if (coll? v)
       (let [diff (into [] (set/difference (into #{} (a k)) (into #{} v)))]
         (if (empty? diff)
           (dissoc a k)
           (update a k #(into [] (set/difference (into #{} %) (into #{} v))))))
       (if (contains? a k) (dissoc a k) a)))
   conf
   pkgs))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;; NOTE: Stateful functions beyond this point ;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro suppress-stdout [verbose? & body]
  `(if (not ~verbose?)
     (do ~@body)
     (binding [*out* (proxy [~'java.io.StringWriter] []
                       (write
                         ([~'str] nil)
                         ([~'str ~'off ~'len] nil)))]
       ~@body)))

(defn prompt [prompt-string options]
  (try
    (println (str prompt-string "\n"
                  (->> options
                       (map-indexed (fn [k v] (str "- " (name v) " (" k ")")))
                       (str/join "\n"))))
    (or (get options (Integer/parseInt (read-line)))
        (last options))                 ; Get the last if out of range
    (catch java.lang.NumberFormatException _
      (error "Please enter a number from the selection.")
      (prompt prompt-string options))))

(defn yes-or-no
  "Basic yes or no prompt."
  [default & prompt]
  (print (str (str/join " " prompt) "? " (if default "Y/n" "N/y")) " ")
  (flush)
  (let [input (read-line)]
    (if (= input "")
      default
      (some #{"y" "yes"} [(str/lower-case input)]))))

(defn sh-verbose
  "Launches a subprocess so that the command can take over stdin/stdout"
  [& command]
  (let [process (ProcessBuilder. command)
        inherit (java.lang.ProcessBuilder$Redirect/INHERIT)]
    (doto process
      (.redirectOutput inherit)
      (.redirectError inherit)
      (.redirectInput inherit))
    (.waitFor (.start process))))

(defn read-edn [file-name]
  (try
    (->> file-name slurp .getBytes io/reader java.io.PushbackReader. edn/read deserialize)
    (catch java.io.FileNotFoundException _
      nil)))

(defn write-edn [file-name edn]
  (let [contents (serialize edn)]
    (try
      (pprint contents (io/writer file-name))
      contents
      (catch java.io.FileNotFoundException _
        nil))))

(defn get-new []
  (try
    (read-edn c/conf-file)
    (catch java.lang.RuntimeException _
      {})))

(defn get-temp []
  (try
    (read-edn c/temp-file)
    (catch java.lang.RuntimeException _ {})
    (catch java.io.FileNotFoundException _
      (-> c/data-dir java.io.File. .mkdir)
      (-> c/temp-file java.io.File. .createNewFile)
      (spit c/temp-file "{}")
      {})))

(defn get-old []
  (try
    (read-edn c/gen-file)
    (catch java.lang.RuntimeException _ [{}])
    (catch java.io.FileNotFoundException _
      (-> c/data-dir java.io.File. .mkdir)
      (-> c/gen-file java.io.File. .createNewFile)
      (spit c/gen-file "[]")
      [])))

(defn add-generation [new-conf]
  (try
    (let [old (conj (get-old) new-conf)]
      (write-edn c/gen-file old))
    (catch java.io.FileNotFoundException _
      (println "Error occured creating a new generation."))))

(defn remove-generation []
  (try
    (let [old (pop (get-old))]
      (if (empty? old)
        (write-edn c/gen-file [{}])
        (write-edn c/gen-file old)))
    (catch java.io.FileNotFoundException _
      (println "Error occured creating a new generation."))))

(defn write-to-conf [contents] (write-edn c/conf-file contents))
(defn write-to-temp [contents] (write-edn c/temp-file contents))

(defn add-to-temp
  ([install-map] (add-to-temp (get-temp) install-map))
  ([config install-map] (write-edn c/temp-file (merge-conf config install-map))))

(defn add-to-conf
  ([install-map] (add-to-conf (get-new) install-map))
  ([config install-map]
   (let [gen (merge-conf config install-map)]
     (add-generation (dissoc gen :default-manager))
     (write-edn c/conf-file gen))))

(defn remove-from-temp
  ([remove-map] (remove-from-temp (get-temp) remove-map))
  ([config remove-map] (write-edn c/temp-file (remove-from-config config remove-map))))

(defn remove-from-conf
  ([remove-map] (remove-from-conf (get-new) remove-map))
  ([config remove-map]
   (let [gen (remove-from-config config remove-map)]
     (add-generation (dissoc gen :default-manager))
     (write-edn c/conf-file gen))))

;; These are for testing, since I can easily mock them.
(defn already-installed? [installed-fn package] (installed-fn package))
(defn add-archive [add-fn archive] (add-fn archive))
(defn install-package [install-fn package] (install-fn package))
(defn remove-package [remove-fn package] (remove-fn package))

(defn install-all
  "Installs all the packages specified in pkgs using the install-fn"
  [pkgs install-fn installed?]
  (->>
   ;; Put it into a set first to avoid doing the same thing multiple times
   (into (ordered-set) pkgs)
   ;; Filter out any accidental nils
   (filter #(not (nil? %)))
   ;; Install all other packages
   (mapv (fn [p]
           (if (already-installed? installed? p)
             (or (println (bold p) (blue "is already installed")) p)
             (do
               (print (str "Installing " (bold p) "... "))
               (flush)
               (let [out (install-package install-fn p)]
                 (if out
                   (do (-> "Successfully installed!" green println) p)
                   (-> "Failed to install" red println)))))))))

(defn add-all-archives [archives add-fn]
  (->>
   (into (ordered-set) archives)
   (mapv (fn [a]
           (-> (str "Adding " a "... ") bold print)
           (flush)
           (when-not (nil? a)
             (let [out (add-archive add-fn (str a))]
               (println out)
               (if out
                 (or (-> "Successfully added!" green println) a)
                 (-> "Failed to add." red println))))))))

(defn remove-all
  "Removes all the packages specified in pkgs using the remove-fn"
  [pkgs remove-fn installed?]
  (->>
   ;; Avoid doing the same thing twice
   (into (ordered-set) pkgs)
   ;; Uninstall all other packages
   (mapv (fn [p]
           (-> (str "Uninstalling " p "... ") bold print)
           (flush)
           (cond
             (not (already-installed? installed? p)) (println "it is already uninstalled.")
             (nil? p) false
             :else
             (let [out (remove-package remove-fn (str p))]
               (println out)
               (if out
                 (or (-> "Successfully uninstalled!" green println) p)
                 (-> "Failed to uninstall" red println))))))))
