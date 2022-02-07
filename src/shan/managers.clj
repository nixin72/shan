(ns shan.managers
  (:require
   [clojure.java.shell :as shell]
   [shan.print :as p]
   [shan.config :as c]
   [shan.util :as u]
   [shan.managers.installed :as installed]
   [shan.managers.list :as list]))

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
            :sudo? true
            :install "sudo -S -- pacman -S --noconfirm"
            :remove "sudo -S -- pacman -R --noconfirm"
            :installed? "pacman -Q"}
   :paru {:type :system
          :uses :pacman
          :list list/pacman
          :sudo? true
          :install "paru --sudoflags -S -S --noconfirm"
          :remove "paru --sudoflags -S -R --noconfirm"
          :installed? "paru -Q"}
   :yay {:type :system
         :uses :pacman
         :sudo? true
         :list list/pacman
         :install "yay --sudoflags -S -S --noconfirm"
         :remove "yay --sudoflags -S -R --noconfirm"
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
         :install "gem install"
         :remove "gem uninstall -x"
         :installed? "gem list -lie"}
   :raco {:type :language
          :list list/raco
          :remove "raco pkg remove"
          :install "raco pkg install"
          :installed? installed/raco?}
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
      (p/fatal-error "No known package managers available on your system")

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
         true "Would you like to set" (p/bold (name selected)) "as the default package manager")]
    [selected set-selected-as-default?]))
