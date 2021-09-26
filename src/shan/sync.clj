(ns shan.sync
  (:require
   [clojure.data :as data]
   [shan.util :as u]
   [shan.managers.managers :as pm]))

(defn sync-conf [{:keys [verbose]}]
  (let [old-config (u/get-old)
        last-config (last old-config)
        new-config (dissoc (u/get-new) :default)
        [add del] (data/diff new-config last-config)
        del (dissoc del :default)]

    (when-not (= [add del] [nil nil])
      (u/add-generation new-config))

    [(reduce-kv #(assoc %1 %2 (when %3 (pm/install %2 %3 verbose))) {} new-config)
     (reduce-kv #(assoc %1 %2 (when %3 (pm/delete %2 %3 verbose))) {} del)]))
