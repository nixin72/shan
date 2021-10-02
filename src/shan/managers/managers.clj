(ns shan.managers.managers
  (:require
   [clojure.string :as str]
   [clojure.java.shell :as shell]
   [shan.managers.npm :as npm]
   [shan.config :as c]
   [shan.util :as u]))

(def package-managers
  (->> {:brew {:install "brew install"
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
              :installed? "gem list -lie"}}
       (reduce-kv
        (fn [a k v]
          (cond c/windows? (= 0 (:exit (shell/sh "which" (name k))))
                (= 0 (:exit (shell/sh "which" (name k)))) (assoc a k v)
                :else a))
        {})))

(defn make-fn [command verbose?]
  (cond
    (fn? command) command
    (string? command) (make-fn (str/split command #" ") verbose?)
    (vector? command) #(let [out (apply shell/sh (conj command (str %)))]
                         (when verbose?
                           (println "\n" out))
                         (= 0 (:exit out)))))

(defn install-pkgs [manager pkgs verbose?]
  (let [pm (get package-managers manager)
        {:keys [install installed?]} pm]
    (println "Installing" (u/bold (name manager)) "packages:")
    (let [out (u/install-all
               pkgs (make-fn install verbose?) (make-fn installed? verbose?))]
      (println "")
      (zipmap (map #(str install " " %) pkgs) out))))

(defn remove-pkgs [manager pkgs verbose?]
  (let [pm (get package-managers manager)
        {:keys [remove installed?]} pm]
    (println "Uninstalling" (u/bold (name manager)) "packages:")
    (let [out (u/remove-all
               pkgs (make-fn remove verbose?) (make-fn installed? verbose?))]
      (println "")
      (zipmap (map #(str remove " " %) pkgs) out))))

(defn installed-with [pkg]
  (reduce-kv
   (fn [a k v]
     (let [installed? (make-fn (:installed? v) false)]
       (if (installed? pkg)
         (conj a k) a)))
   []
   package-managers))

(defn exists? [manager]
  (if c/windows?
    false
    (= 0 (:exit (shell/sh "which" manager)))))
