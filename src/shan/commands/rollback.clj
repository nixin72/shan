(ns shan.commands.rollback
  (:require
   [clojure.data :as data]
   [shan.managers :as pm]
   [shan.print :as p]
   [shan.util :as u]
   [shan.config :as c]))

(defn- cli-rollback [{:keys [check]}]
  (let [old-config (u/get-old)
        [new-config last-gen] (take-last 2 old-config)
        [add del] (data/diff new-config last-gen)
        add (dissoc add :default-manager)
        del (dissoc del :default-manager)]

    (when-not (= [add del] [nil nil])
      (u/remove-generation))

    (if c/testing?
      [(reduce-kv #(assoc %1 %2 (when %3 (pm/install-pkgs %2 %3 check))) {} add)
       (reduce-kv #(assoc %1 %2 (when %3 (pm/remove-pkgs %2 %3 check))) {} del)]
      p/exit-code)))

(def command
  {:command "rollback"
   :short "rb"
   :arguments 0
   :description "Roll back your last change"
   :runs cli-rollback})
