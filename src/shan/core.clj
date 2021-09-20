(ns shan.core
  (:require
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [clojure.java.shell :as shell]
   [clojure.data :as data]
   [shan.managers.managers :as pm])
  (:gen-class))

(set! *warn-on-reflection* true)

(def proj-name "shan")
(def home (System/getenv "HOME"))
(def config-name (str home "/.config/" proj-name ".edn"))
(def gen-dir (str home "/.local/share/" proj-name))
(def gen-file (str gen-dir "/generations.edn"))

(defn read-edn [file-name]
  (-> (slurp file-name)
      .getBytes
      io/reader
      java.io.PushbackReader.
      edn/read))

(defn get-new []
  (read-edn config-name))

(defn get-old []
  (try
    (read-edn gen-file)
    (catch java.io.FileNotFoundException _
      (-> gen-dir java.io.File. .mkdir)
      (-> gen-file java.io.File. .createNewFile)
      (spit gen-file "[]")
      [])))

(defn add-generation [new-conf]
  (try
    (spit gen-file (str (conj (get-old) new-conf)))
    (get-old)
    (catch java.io.FileNotFoundException _)))

(defn cli-sync []
  (let [old-config (get-old)
        last-config (last old-config)
        new-config (get-new)
        [add del] (data/diff last-config new-config)]

    (when-not (= [add del] [nil nil])
      (add-generation new-config))

    (prn [(reduce-kv #(assoc %1 %2 (pm/install %2 %3)) {} add)
          (reduce-kv #(assoc %1 %2 (pm/delete %2 %3)) {} del)])))

(defn cli-rollback [] :rollback)

(defn cli-upgrade [] :upgrade)

(defn cli-install [] :install)

(defn cli-remove [] :remove)

(defn cli-list [] :list)

(defn cli-config []
  (->> (shell/sh "/bin/sh" "-c" "vim" config-name) :out))

(defn -main [& [command & args]]
  (case command
    "sync" (cli-sync)
    "rb" (cli-rollback)
    "up" (cli-upgrade)
    "in" (cli-install)
    "rm" (cli-remove)
    "ls" (cli-list)
    "ed" (cli-config)
    "tm" (cli-config)
    (cli-sync)))
