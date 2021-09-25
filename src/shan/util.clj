(ns shan.util
  (:require
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [clojure.pprint :refer [pprint]]
   [clojure.data :as data]
   [clojure.string :as str]
   [shan.config :as c]))

(defn read-edn [file-name]
  (-> file-name slurp .getBytes io/reader java.io.PushbackReader. edn/read))

(read-edn c/temp-file)

(defn write-edn [file-name edn]
  (pprint edn (clojure.java.io/writer file-name)))

(defn get-new []
  (read-edn c/conf-file))

(defn get-temp []
  (try
    (read-edn c/temp-file)
    (catch java.io.FileNotFoundException _
      (-> c/gen-dir java.io.File. .mkdir)
      (-> c/temp-file java.io.File. .createNewFile)
      (spit c/temp-file "{}")
      {})))

(defn get-old []
  (try
    (read-edn c/gen-file)
    (catch java.io.FileNotFoundException _
      (-> c/gen-dir java.io.File. .mkdir)
      (-> c/gen-file java.io.File. .createNewFile)
      (spit c/gen-file "[]")
      [])))

(defn add-generation [new-conf]
  (try
    (write-edn c/gen-file (conj (get-old) new-conf))
    (let [old (get-old)
          num-gens (count old)]
      (println "Creating generation" num-gens))
    (catch java.io.FileNotFoundException _
      (println "Error occured creating a new generation."))))

(defn merge-conf
  ([conf] conf)
  ([conf1 conf2]
   (let [[old new] (data/diff conf1 conf2)]
     (reduce-kv (fn [a k v]
                  (if (contains? a k)
                    (update a k into v)
                    (assoc a k v)))
                old
                new)))
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

(defn add-to-temp [install-map]
  (write-edn c/temp-file (merge-conf (get-temp) install-map)))

(defn add-to-conf [install-map]
  (write-edn c/conf-file (merge-conf (get-new) install-map)))

(defn red [string]
  (str "\033[1;31m" string "\033[0m"))

(defn yellow [string]
  (str "\033[1;33m" string "\033[0m"))

(defn green [string]
  (str "\033[1;32m" string "\033[0m"))

(defn blue [string]
  (str "\033[1;34m" string "\033[0m"))

(defn bold [string]
  (str "\033[0;1m" string "\033[0m"))

(defn warning [arg]
  (println (-> "Warning: " yellow bold) arg))

(defn error [arg]
  (println (-> "Error: " red bold) arg))
