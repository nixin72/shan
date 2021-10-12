(ns shan.sync
  (:require
   [clojure.data :as data]
   [shan.util :as u]
   [shan.config :as c]
   [shan.managers :as pm]))

(defn cli-sync [{:keys [verbose]}]
  (let [old-config (u/get-old)
        last-config (last old-config)
        new-config (dissoc (u/get-new) :default-manager)
        [add del] (data/diff new-config last-config)
        del (dissoc del :default-manager)]

    (when-not (= [add del] [nil nil])
      (u/add-generation new-config))

    (if c/testing?
      [(reduce-kv #(assoc %1 %2 (when %3 (pm/install-pkgs %2 %3 verbose))) {} add)
       (reduce-kv #(assoc %1 %2 (when %3 (pm/remove-pkgs %2 %3 verbose))) {} del)]
      u/exit-code)))
