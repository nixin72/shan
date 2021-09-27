(ns shan.managers.managers
  (:require
   [clojure.string :as str]
   [clojure.java.shell :as shell]
   [shan.managers.npm :as npm]
   [shan.util :as u]))

(def package-managers
  {:npm {:install "npm install --global"
         :remove "npm uninstall --gloabl"
         :installed? npm/installed?}
   :yay {:install "yay -S --noconfirm"
         :remove "yay -R --noconfirm"
         :installed? "yay -Q"}
   :pip {:install "python -m pip install"
         :remove "python -m pip uninstall -y"
         :installed? "python -m pip show"}
   :gem {:install "gem install"
         :remove "gem uninstall -x"
         :installed? "gem list -lie"}})

(defn make-fn [command verbose?]
  (cond
    (fn? command) command
    (string? command) (make-fn (str/split command #" ") verbose?)
    (vector? command) #(let [out (apply shell/sh (conj command (str %)))]
                         (when verbose?
                           (println "\n" out))
                         (= 0 (:exit out)))))

(defn install [manager pkgs verbose?]
  (let [pm (get package-managers manager)
        {:keys [install installed?]} pm]
    (println "Installing" (u/bold (name manager)) "packages:")
    (let [out (u/install-all
               pkgs (make-fn install verbose?) (make-fn installed? verbose?))]
      (println "")
      out)))

(defn delete [manager pkgs verbose?]
  (let [pm (get package-managers manager)
        {:keys [remove installed?]} pm]
    (println "Uninstalling" (u/bold (name manager)) "packages:")
    (let [out (u/delete-all
               pkgs (make-fn remove verbose?) (make-fn installed? verbose?))]
      (println "")
      out)))

(defn installed-with [pkg]
  (reduce-kv
   (fn [a k v]
     (let [installed? (make-fn (:installed? v) false)]
       (if (installed? pkg)
         (conj a k) a)))
   []
   package-managers))
