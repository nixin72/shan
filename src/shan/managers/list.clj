(ns shan.managers.list
  (:require
   [clojure.data.json :as json]
   [clojure.string :as str]
   [clojure.java.shell :as sh]
   [shan.util :as u]))

(def preserve-prompt "Would you like to preserve version numbers for")

(defn brew []
  (let [preserve-versions? (u/yes-or-no false preserve-prompt (u/bold "brew"))
        packages (->> (sh/sh "brew" "list" "--versions")
                      :out
                      str/split-lines
                      (map #(str/split % #" "))
                      (map #(hash-map (symbol (first %)) (last %))))]
    (if preserve-versions? packages (into [] (keys packages)))))

(defn pacman []
  (->> (sh/sh "pacman" "-Qqett") :out str/split-lines (into [])))

(defn npm []
  (let [preserve-versions? (u/yes-or-no false preserve-prompt (u/bold "npm"))
        packages
        (some->>
         (sh/sh "npm" "list" "-g" "--depth" "0" "--json" "true")
         (:out)
         (json/read-str)
         (#(dissoc (get % "dependencies") "npm"))
         ;; This wack shit here is cause NPM decided to go and change the format
         ;; that `npm list --json true` outputs things in. Before, it was
         ;; {:dependencies
         ;;   [{"from" "<package-name>"
         ;;     "version" "<version>"}
         ;;    {"from" "<package-name2>"
         ;;     "version" "<version2>"}]}
         ;; But then they went and changed it to
         ;; {:dependencies
         ;;  {"<package-name>" {"version" "<version>"}
         ;;   "<package-name2>" {"version" "<version2>"}}}
         ;; Which makes a lot more sense, but I'm still annoyed this did it.
         ;; So that's what this weird thing here handles, it handles compatibility
         ;; with older versions of NPM.
         ((fn [deps]
            (if (map? deps)
              (reduce-kv #(assoc %1 %2 (%3 "version")) {} deps)
              (->> (vals deps)
                   (map #(hash-map (symbol (% "from")) (% "version")))
                   (apply merge))))))]

    (if preserve-versions? packages (into [] (keys packages)))))

(defn pip []
  (let [preserve-versions? (u/yes-or-no false preserve-prompt (u/bold "pip"))
        packages
        (->> (sh/sh "python" "-m" "pip" "list" "--user" "--exclude" "pip" "--format" "json")
             :out
             json/read-str
             (mapv #(hash-map (symbol (% "name")) (% "version")))
             (apply merge))]
    (if preserve-versions? packages (into [] (keys packages)))))

(defn gem []
  (let [preserve-versions? (u/yes-or-no false preserve-prompt (u/bold "gem"))
        packages
        (some->> (sh/sh "gem" "list" "--local")
                 :out
                 str/split-lines
                 (map #(str/split % #" "))
                 (map #(hash-map (symbol (first %)) (str/replace (last %) #"[\(\)]" "")))
                 (apply merge))]
    (if preserve-versions? packages (into [] (keys packages)))))
