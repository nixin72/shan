(ns shan.packages
  (:require
   [clojure.string :as str]
   [clojure.java.shell :as shell]
   [shan.print :as p]
   [shan.config :as c]
   [shan.util :as u]
   [shan.cache :as cache]
   [shan.managers :as pm]))

(def password (atom nil))

(defn make-fn
  "Generate a function that executes a shell command."
  [command verbose? & {:keys [sudo?]}]
  (cond
    (fn? command) command
    (string? command) (make-fn (str/split command #" ") verbose? :sudo? sudo?)
    (vector? command) #(let [cmd (into command [(str %) :in (when sudo? @password)])
                             out (if verbose?
                                   (apply u/sh-verbose cmd)
                                   (apply shell/sh cmd))]
                         (if c/testing?
                           (:out out)
                           (= 0 (:exit out))))))

(defn unknown-package-manager [manager]
  (p/error (p/bold (name manager))
           " is not a package manager known by shan.\n"
           "Please make an issue on our GitHub repository if you want it to be included."))

(defn unavailable-package-manager [manager]
  (p/error (p/bold (name manager))
           " does not appear to be installed on your system.\n"
           "If it is, please ensure that it's available in your $PATH.\n"
           "If it is in your path and this still isn't working, please make an issue on our GitHub repository."))

(defmacro with-package-manager [[manager-details package-manager] & body]
  `(let [~manager-details (get (pm/installed-managers) ~package-manager)]
     (if (nil? ~manager-details)
       (if (contains? pm/package-managers ~manager-details)
         (unavailable-package-manager ~package-manager)
         (unknown-package-manager ~package-manager))
       (do ~@body))))

(defmacro with-sudo [pm & body]
  `(do (when (and (:sudo? ~pm) (nil? @password))
         (let [pass# (.readPassword (System/console) "Enter password: " nil)]
           (reset! password pass#)))
       ~@body))

(defn install-pkgs [manager pkgs verbose?]
  (with-package-manager [pm manager]
    (when (contains? pm :pre-install)
      (apply (make-fn (:pre-install pm) verbose?) []))

    (with-sudo pm
      (p/logln "Installing" (p/bold (name manager)) "packages:")
      (let [{:keys [install installed?]} pm
            out (u/install-all
                 pkgs
                 (make-fn install verbose? :sudo? (:sudo? pm))
                 (make-fn installed? false :sudo? (:sudo? pm)))]
        (newline)
        (zipmap (map #(str install " " %) pkgs) out)))))

(defn add-archives [manager ppas verbose?]
  (with-package-manager [pm manager]
    (p/logln "Adding package archives for:" (p/bold (name manager)))
    (let [{:keys [add-ppas]} pm
          out (u/add-all-archives
               ppas
               (make-fn add-ppas verbose?))]
      (newline)
      (zipmap (map #(str add-ppas " " %) ppas) out))))

(defn remove-pkgs [manager pkgs verbose?]
  (with-package-manager [pm manager]
    (with-sudo pm
      (p/logln "Uninstalling" (p/bold (name manager)) "packages:")
      (let [{:keys [remove installed?]} pm
            out (u/remove-all
                 pkgs
                 (make-fn remove verbose? :sudo? (:sudo? pm))
                 (make-fn installed? false :sudo? (:sudo? pm)))]
        (newline)
        (zipmap (map #(str remove " " %) pkgs) out)))))

(defn replace-keys
  "Replaces package managers when one is using another"
  [pkg-map]
  (reduce-kv
   (fn [a k v]
     (let [alias (-> pm/package-managers k :uses)]
       (if alias
         (assoc a alias v)
         (assoc a k v))))
   {}
   pkg-map))

;; TODO: Cache shit to make this go faster, cause it's real slow atm
(defn installed-with
  "Find out what package manager pkg is installed with."
  [pkg]
  (let [pkg (str pkg)
        pkg-cache (cache/read-cache)]
    (reduce-kv
     (fn [a k v]
       (if (some #{pkg} (get pkg-cache k))
         (if (contains? v :uses) a
             (conj a k))
         a))
     []
     (pm/installed-managers))))
