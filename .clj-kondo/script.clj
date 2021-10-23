(ns script
  (:require [clj-kondo.hooks-api :as api]))

(defn let-with-handlers [{:keys [node]}]
  (let [[binding-vec & body] (rest (:children node))
        [sym val err & rest] (:children binding-vec)]
    (when-not (and sym val)
      (throw (ex-info "Binding must contain a binding symbol, a value, and an error handler." {})))
    (let [new-node (api/list-node
                    (list*
                     (api/token-node 'if-let)
                     (api/vector-node [sym val])
                     (let-with-handlers rest body)
                     err))]
      {:node new-node})))
