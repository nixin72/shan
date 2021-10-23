(ns shan.parser)

(defmacro let-with-handlers [bindings & body]
  (if (empty? bindings)
    (do (prn body) `(do ~@body))
    `(if-let ~(into [] (take 2 bindings))
       (let-with-handlers ~(drop 3 bindings) ~@body)
       ~(get bindings 2))))

(let-with-handlers
 [command (first ['ah]) (println "error")]
 (println "good"))

(defn run-cmd [arguments config]
  (let [command (first arguments)
        subcmd (filter #(some #{(:command %) (:short %)} [command])
                       (:subcommands config))
        opts (map #(vector (:option %) (:short %))
                  (:opts subcmd))]
    (prn command subcmd opts)))
