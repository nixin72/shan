(ns shan.managers.list
  (:require
   [clojure.data.json :as json]
   [clojure.string :as str]
   [clojure.java.shell :as sh]
   [flatland.ordered.set :as set]
   [shan.print :as p]
   [shan.util :as u]))

(def preserve-prompt "Would you like to preserve version numbers for")

(defn brew [& {:keys [versions?]}]
  (let [preserve-versions? (if (nil? versions?)
                             (u/yes-or-no false preserve-prompt (p/bold "brew"))
                             versions?)
        packages (->> (sh/sh "brew" "list" "--versions")
                      :out
                      str/split-lines
                      (map #(str/split % #" "))
                      (map #(hash-map (first %) (last %))))]
    (if preserve-versions? packages (into (set/ordered-set) (keys packages)))))

(defn pacman [& _]
  (->> (sh/sh "pacman" "-Qqett") :out str/split-lines (into (set/ordered-set))))

(defn npm [& {:keys [versions?]}]
  (let [preserve-versions? (if (nil? versions?)
                             (u/yes-or-no false preserve-prompt (p/bold "npm"))
                             versions?)
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
                   (map #(hash-map (% "from") (% "version")))
                   (apply merge))))))]

    (if preserve-versions? packages (into (set/ordered-set) (keys packages)))))

(defn pip [& {:keys [versions?]}]
  (let [preserve-versions? (if (nil? versions?)
                             (u/yes-or-no false preserve-prompt (p/bold "pip"))
                             versions?)
        packages
        (->> (sh/sh "python" "-m" "pip" "list" "--user" "--exclude" "pip" "--format" "json")
             :out
             json/read-str
             (mapv #(hash-map (% "name") (% "version")))
             (apply merge))]
    (if preserve-versions? packages (into (set/ordered-set) (keys packages)))))

(defn gem [& {:keys [versions?]}]
  (let [preserve-versions? (if (nil? versions?)
                             (u/yes-or-no false preserve-prompt (p/bold "gem"))
                             versions?)
        packages
        (some->> (sh/sh "gem" "list" "--local")
                 :out
                 str/split-lines
                 (map #(str/split % #" "))
                 (map #(hash-map (first %) (str/replace (last %) #"[\(\)]" "")))
                 (apply merge))]
    (if preserve-versions? packages (into (set/ordered-set) (keys packages)))))

(defn gu [& _]
  (some->> (sh/sh "gu" "list")
           :out
           str/split-lines
           (drop 2)
           (map #(first (str/split % #" ")))
           (into (set/ordered-set))))

(defn raco [& _]
  (some->> (sh/sh "raco" "pkg" "show" "-u")
           :out
           str/split-lines
           (rest)
           (drop-last 1)
           (map #(first (str/split % #" ")))
           (into (set/ordered-set))))
