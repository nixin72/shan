(ns shan.managers
  (:require
   [clojure.string :as str]
   [clojure.java.shell :as shell]
   [shan.managers.installed :as installed]
   [shan.managers.list :as list]
   [shan.config :as c]
   [shan.util :as u]))

(def ^:dynamic package-managers
  ;; TODO: Support MacOS and Windows better
  {:brew {:type :system
          :list list/brew
          :install "brew install"
          :remove "brew uninstall"
          :installed? "brew list"}
   :apt {:type :system
         :pre-install "apt update"
         :install "apt install"
         :add-ppa "add-apt-repository"
         :remove "apt remove"
         :installed? "apt list"}
   :choco {:type :system
           :install "choco install",
           :remove "choco uninstall",
           :installed? "choco list --local-only"}
   :scoop {:type :system
           :install "scoop install"
           :remove "scoop uninstall"
           :installed? "scoop info"}
   :winget {:type :system
            :install "winget install"
            :remove "winget uninstall"
            :installed? "winget list --exact"}
   ;; NOTE: Proper support
   :pacman {:type :system
            :list list/pacman
            :install "sudo pacman -S --noconfirm"
            :remove "sudo pacman -R --noconfirm"
            :installed? "pacman -Q"}
   :paru {:type :system
          :uses :pacman
          :list list/pacman
          :install "paru -S --noconfirm"
          :remove "paru -R --noconfirm"
          :installed? "paru -Q"}
   :yay {:type :system
         :uses :pacman
         :list list/pacman
         :install "yay -S --noconfirm"
         :remove "yay -R --noconfirm"
         :installed? "yay -Q"}
   :npm {:type :language
         :list list/npm
         :install "npm install --global"
         :remove "npm uninstall --global"
         :installed? installed/npm?}
   :yarn {:type :language
          :uses :npm
          :list list/npm
          :install "npm install --global"
          :remove "npm uninstall --global"
          :installed? installed/npm?}
   :pip {:type :language
         :list list/pip
         :install "python -m pip install"
         :remove "python -m pip uninstall -y"
         :installed? "python -m pip show"}
   :gem {:type :language
         :list list/gem
         :remove "gem uninstall -x"
         :installed? "gem list -lie"}
   :gu {:type :language
        :list list/gu
        :remove "gu remove"
        :install "gu install"
        :installed? installed/gu?}})

(defn installed-managers
  "Filter the package managers to just what's installed on the system"
  []
  (let [managers
        (reduce-kv (fn [a k v]
                     (if (if c/windows?
                           (= 0 (:exit (shell/sh "cmd" "/C" "where" (name k))))
                           (= 0 (:exit (shell/sh "which" (name k)))))
                       (assoc a k v)
                       a))
                   {} package-managers)]
    (if (empty? managers)
      (do (u/error "No known package managers available on your system")
          (System/exit -1))
      managers)))

(defn set-of-package-managers
  "Get a set of the names of package managers"
  []
  (->> (installed-managers) (keys) (map name) (into #{})))

;; TODO: Figure out how to test
(defn determine-default-manager
  "Try to determine what the default manager should be if there's no
  :default-manager set and there's no packages installed.
  1. Find if there's only one system package manager. If so, use it.
  2. If there's two system package managers, ask for a default.
  3. If there's no system package managers, prompt for another one."
  []
  (let [mans (installed-managers)
        {:keys [system language]}
        (reduce-kv (fn [a k v]
                     (if (= (:type v) :system)
                       (update a :system conj k)
                       (update a :language conj k)))
                   {:system [] :language []}
                   mans)
        selected
        (cond
          (= (count system) 1) system
          (> (count system) 1) (u/prompt
                                (str "Several system package managers were found. "
                                     "Which one would you like to use")
                                system)
          :else (u/prompt
                 (str "No system package managers could be found. "
                      "Which other package manager would you like to use")
                 language))
        set-selected-as-default?
        (u/yes-or-no
         true "Would you like to set" (u/bold (name selected)) "as the default package manager")]
    [selected set-selected-as-default?]))

(defn make-fn
  "Generate a function that executes a shell command."
  [command verbose?]
  (cond
    (fn? command) command
    (string? command) (make-fn (str/split command #" ") verbose?)
    (vector? command) #(let [cmd (conj command (str %))
                             out (if verbose?
                                   (apply u/sh-verbose cmd)
                                   (apply shell/sh cmd))]
                         (if c/testing?
                           (:out out)
                           (= 0 (:exit out))))))

(defn unknown-package-manager [manager]
  (u/error (str (u/bold (name manager)) " is not a package manager known by shan.\n"
                "Please make an issue on our GitHub repository if you want it to be included.")))

(defn unavailable-package-manager [manager]
  (u/error (str (u/bold (name manager)) " does not appear to be installed on your system.\n"
                "If it is, please ensure that it's available in your $PATH.\n"
                "If it is in your path and this still isn't working, please make an issue on our GitHub repository.")))

(defmacro with-package-manager [[manager-details package-manager] & body]
  `(let [~manager-details (get (installed-managers) ~package-manager)]
     (if (nil? ~manager-details)
       (if (contains? package-managers ~manager-details)
         (unavailable-package-manager ~package-manager)
         (unknown-package-manager ~package-manager))
       (do ~@body))))

(defn install-pkgs [manager pkgs verbose?]
  (with-package-manager [pm manager]
    (when (contains? pm :pre-install)
      (apply (make-fn (:pre-install pm) verbose?) []))

    (println "Installing" (u/bold (name manager)) "packages:")
    (let [{:keys [install installed?]} pm
          out (u/install-all
               pkgs (make-fn install verbose?) (make-fn installed? false))]
      (println "")
      (zipmap (map #(str install " " %) pkgs) out))))

(defn add-archives [manager ppas verbose?]
  (with-package-manager [pm manager]
    (println "Adding package archives for:" (u/bold (name manager)))
    (let [{:keys [add-ppas]} pm
          out (u/add-all-archives ppas (make-fn add-ppas verbose?))]
      (println "")
      (zipmap (map #(str add-ppas " " %) ppas) out))))

(defn remove-pkgs [manager pkgs verbose?]
  (with-package-manager [pm manager]

    (let [{:keys [remove installed?]} pm
          _ (println "Uninstalling" (u/bold (name manager)) "packages:")
          out (u/remove-all
               pkgs (make-fn remove verbose?) (make-fn installed? false))]
      (println "")
      (zipmap (map #(str remove " " %) pkgs) out))))

(defn replace-keys
  "Replaces package managers when one is using another"
  [pkg-map]
  (reduce-kv
   (fn [a k v]
     (let [alias (-> package-managers k :uses)]
       (if alias
         (assoc a alias v)
         (assoc a k v))))
   {}
   pkg-map))

;; TODO: Cache shit to make this go faster, cause it's real slow atm
(defn installed-with
  "Find out what package manager pkg is installed with."
  [pkg]
  (reduce-kv
   (fn [a k v]
     (if (-> (:installed? v) (make-fn false) (u/already-installed? pkg))
       (if (contains? v :uses) a
           (conj a k))
       a))
   []
   (installed-managers)))
