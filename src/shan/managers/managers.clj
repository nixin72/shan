(ns shan.managers.managers
  (:require
   [clojure.string :as str]
   [clojure.java.shell :as shell]
   [shan.managers.npm :as npm]
   [shan.config :as c]
   [shan.util :as u]))

(def package-managers
  {:brew {:install "brew install"
          :remove "brew uninstall"
          :installed? "brew list"}
   :yay {:install "yay -S --noconfirm"
         :remove "yay -R --noconfirm"
         :installed? "yay -Q"}
   :npm {:install "npm install --global"
         :remove "npm uninstall --global"
         :installed? npm/installed?}
   :pip {:install "python -m pip install"
         :remove "python -m pip uninstall -y"
         :installed? "python -m pip show"}
   :gem {:install "gem install"
         :remove "gem uninstall -x"
         :installed? "gem list -lie"}})

(def installed-managers
  (reduce-kv (fn [a k v]
               (if (if c/windows?
                     (= 0 (:exit (shell/sh "cmd" "/C" "where" (name k))))
                     (= 0 (:exit (shell/sh "which" (name k)))))
                 (assoc a k v)
                 a))
             {} package-managers))

(defn make-fn [command verbose?]
  (cond
    (fn? command) command
    (string? command) (make-fn (str/split command #" ") verbose?)
    (vector? command) #(let [out (apply shell/sh (conj command (str %)))]
                         (when verbose?
                           (println "\n" out))
                         (= 0 (:exit out)))))

(defn unknown-package-manager [manager]
  (u/error (str (u/bold (name manager)) " is not a package manager known by shan.\n"
                "Please make an issue on our GitHub repository if you want it to be included.")))

(defn unavailable-package-manager [manager]
  (u/error (str (u/bold (name manager)) " does not appear to be installed on your system.\n"
                "If it is, please ensure that it's available in your $PATH.\n"
                "If it is in your path and this still isn't working, please make an issue on our GitHub repository.")))

(defn install-pkgs [manager pkgs verbose?]
  (let [pm (get installed-managers manager)]
    (if (nil? pm)
      (if (contains? package-managers manager)
        (unavailable-package-manager manager)
        (unknown-package-manager manager))
      (let [{:keys [install installed?]} pm
            _ (println "Installing" (u/bold (name manager)) "packages:")
            out (u/install-all
                 pkgs (make-fn install verbose?) (make-fn installed? verbose?))]
        (println "")
        (zipmap (map #(str install " " %) pkgs) out)))))

(defn remove-pkgs [manager pkgs verbose?]
  (let [pm (get package-managers manager)]
    (if (nil? pm)
      (if (contains? package-managers manager)
        (unavailable-package-manager manager)
        (unknown-package-manager manager))
      (let [{:keys [remove installed?]} pm
            _ (println "Uninstalling" (u/bold (name manager)) "packages:")
            out (u/remove-all
                 pkgs (make-fn remove verbose?) (make-fn installed? verbose?))]
        (println "")
        (zipmap (map #(str remove " " %) pkgs) out)))))

(defn installed-with [pkg]
  (reduce-kv
   (fn [a k v]
     (let [installed? (make-fn (:installed? v) false)]
       (if (installed? pkg)
         (conj a k) a)))
   []
   package-managers))
