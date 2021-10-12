(ns shan.util
  (:require
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [clojure.pprint :refer [pprint]]
   [clojure.data :as data]
   [clojure.set :as set]
   [clojure.string :as str]
   [shan.config :as c]))

(defn deserialize [config-map]
  (reduce-kv
   #(assoc %1 %2 (cond
                   (keyword? %3) %3
                   (map? %3) (reduce-kv (fn [a k v] (assoc a (symbol k) v)) {} %3)
                   (vector? %3) (mapv symbol %3)))
   {}
   config-map))

(defn serialize [config-map]
  (reduce-kv
   #(assoc %1 %2 (cond
                   (keyword? %3) %3
                   (map? %3)
                   (reduce-kv
                    (fn [a k v] (assoc a (if (= (first (str k)) \@) (str k) k) v))
                    {} %3)
                   (vector? %3)
                   (mapv (fn [v] (if (= (first (str v)) \@) (str v) v)) %3)))
   {}
   config-map))

(defn do-merge [conf1 conf2]
  (reduce-kv (fn [a k v]
               (if (contains? a k)
                 (update a k #(into [] (into (into #{} v) %)))
                 (assoc a k v)))
             conf1
             conf2))

(defn merge-conf
  ([] {})
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
                 (let [sym (symbol v)
                       kw (keyword (subs v 1))]
                   (cond
                     (= v (str kw)) (into a [kw []])
                     (= a []) (into a [default-key [sym]])
                     :else (conj (pop a) (conj (last a) sym)))))
               []
               vector-map)
       (partition 2)
       (sort-by first)
       (reduce (fn [a [k v]]
                 (if (contains? a k)
                   (update a k into v)
                   (assoc a k v)))
               {})))

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

(def exit-code (atom 0))
(def normal "\033[0m")
(defn red [string]    (str "\033[0;31m" string normal))
(defn green [string]  (str "\033[0;32m" string normal))
(defn yellow [string] (str "\033[0;33m" string normal))
(defn blue [string]   (str "\033[0;34m" string normal))
(defn grey [string]   (str "\033[0;37m" string normal))
(defn bold [string]   (str "\033[0;1m"  string normal))
(defn warning [arg]   (println (-> "Warning:" yellow bold) arg))
(defn error [& arg]
  (reset! exit-code 1)
  (println (-> "Error:" red bold) (str/join " " arg)))

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

(defn yes-or-no [default & prompt]
  (print (str (str/join " " prompt) "? " (if default "Y/n" "N/y")) " ")
  (flush)
  (let [input (read-line)]
    (if (= input "")
      default
      (some #{"y" "yes"} [(str/lower-case input)]))))

(defn read-edn [file-name]
  (try
    (->> file-name slurp .getBytes io/reader java.io.PushbackReader. edn/read
         ((fn [x]
            (if (vector? x)
              (mapv deserialize x)
              (deserialize x)))))
    (catch java.io.FileNotFoundException _
      nil)))

(defn write-edn [file-name edn]
  (let [contents (if (vector? edn) (mapv serialize edn) (serialize edn))]
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
      (-> c/gen-dir java.io.File. .mkdir)
      (-> c/temp-file java.io.File. .createNewFile)
      (spit c/temp-file "{}")
      {})))

(defn get-old []
  (try
    (read-edn c/gen-file)
    (catch java.lang.RuntimeException _ [{}])
    (catch java.io.FileNotFoundException _
      (-> c/gen-dir java.io.File. .mkdir)
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
      (prn old (empty? old))
      (if (empty? old)
        (write-edn c/gen-file [{}])
        (write-edn c/gen-file old)))
    (catch java.io.FileNotFoundException _
      (println "Error occured creating a new generation."))))

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

(defn install-all [pkgs install-fn installed?]
  (->>
   ;; Put it into a set first to avoid doing the same thing multiple times
   (into #{} pkgs)
   ;; Filter out any accidental nils
   (filter #(not (nil? %)))
   ;; Install all other packages
   (mapv (fn [p]
           (if (installed? p)
             (or (println (bold p) (blue "is already installed")) p)
             (do
               (print (str "Installing " (bold p) "... "))
               (flush)
               (let [out (install-fn p)]
                 (if out
                   (do (-> "Successfully installed!" green println) p)
                   (-> "Failed to install" red println)))))))))

(defn add-all-archives [archives add-fn]
  (->>
   (into #{} archives)
   (mapv (fn [a]
           (-> (str "Adding " a "... ") bold print)
           (flush)
           (when-not (nil? a)
             (let [out (add-fn (str a))]
               (println out)
               (if out
                 (or (-> "Successfully added!" green println) a)
                 (-> "Failed to add." red println))))))))

(defn remove-all [pkgs remove-fn installed?]
  (->>
   ;; Avoid doing the same thing twice
   (into #{} pkgs)
   ;; Uninstall all other packages
   (mapv (fn [p]
           (-> (str "Uninstalling " p "... ") bold print)
           (flush)
           (cond
             (not (installed? p)) (println "it is already uninstalled.")
             (nil? p) false
             :else
             (let [out (remove-fn (str p))]
               (println out)
               (if out
                 (or (-> "Successfully uninstalled!" green println) p)
                 (-> "Failed to uninstall" red println))))))))
