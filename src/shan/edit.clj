(ns shan.edit
  (:import [java.lang ProcessBuilder])
  (:require
   [shan.config :as c]
   [shan.util :as u]
   [shan.managers :as pm]))

(defn cli-edit [& _]
  (let [process (ProcessBuilder. [(System/getenv "EDITOR") c/conf-file])
        inherit (java.lang.ProcessBuilder$Redirect/INHERIT)]
    (doto process
      (.redirectOutput inherit)
      (.redirectError inherit)
      (.redirectInput inherit))
    (.waitFor (.start process))))

(defn cli-purge [{:keys [verbose]}]
  (let [remove-map (u/get-temp)
        result (reduce-kv #(assoc %1 %2 (pm/remove-pkgs %2 %3 verbose))
                          {}
                          remove-map)]
    (spit c/temp-file "{}")
    result))

(defn cli-merge [{:keys []}]
  (let [temp (u/get-temp)]
    (u/add-to-conf temp)
    (spit c/temp-file "{}")))
