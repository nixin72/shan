(ns shan.commands.remove
  (:require
   [clojure.java.shell]
   [shan.print :as p]
   [shan.util :as u]
   [shan.config :as c]
   [shan.packages :as ps]
   [shan.commands.-options :as opts]
   [shan.cache :as cache]))

(defn- remove-with-pm-from-list [pms pkg]
  (let [pm (u/prompt (str "Which package manager(s) would you like to remove "
                          (p/blue pkg)  " using?\n")
                     (conj pms :all))]
    (if (= pm :all)
      (reduce #(assoc %1 %2 [pkg]) {} pms)
      {pm [pkg]})))

(defn- remove-with-pm-from-installed [pkg]
  (let [pms (ps/installed-with pkg)]
    (cond
      (= (count pms) 0)
      (do (p/warnln "Package '"
                    (p/bold pkg)
                    "' isn't installed in any package manager known by Shan.")
          {})
      (= (count pms) 1) {(first pms) [pkg]}
      :else (remove-with-pm-from-list pms pkg))))

(defn- find-package-manager [conf pkgs]
  (apply
   u/merge-conf
   (mapv (fn [pkg]
           (let [pms (reduce-kv #(if (some #{pkg} %3) (conj %1 %2) %1) [] conf)]
             (cond
               (= (count pms) 1) {(first pms) [pkg]}
               (= (count pms) 0) (remove-with-pm-from-installed pkg)
               :else (remove-with-pm-from-list pms pkg))))
         pkgs)))

(defn- cli-remove [{:keys [check temp _arguments]}]
  (let [conf (if temp (u/get-temp) (u/get-new))
        remove-map (u/flat-map->map _arguments :default)
        default (find-package-manager (dissoc conf :default-manager)
                                      (:default remove-map))
        remove-map (->> (dissoc remove-map :default)
                        (u/merge-conf default)
                        (ps/replace-keys))
        out (reduce-kv #(assoc %1 %2 (ps/remove-pkgs %2 %3 check))
                       {}
                       remove-map)]
    (cache/remove-from-cache (ps/replace-keys remove-map))
    (if temp
      (u/remove-from-temp conf remove-map)
      (u/remove-from-conf conf remove-map))
    (if c/testing? out p/exit-code)))

(def command
  {:command "remove"
   :short "rm"
   :category "Managing Packages"
   :arguments? *
   :description "Uninstall packages through any supported package manager"
   :examples [opts/context
              {:desc "Removes the packages through yay"
               :ex (str (p/green "shan") " remove python nodejs")}
              {:desc "Removes neovim through yay and react through npm"
               :ex (str (p/green "shan") " remove neovim react")}
              {:desc "Removes atop after prompting the user which manager to use."
               :ex (str (p/green "shan") " remove atop")}
              {:desc "Removes emacs after searching to find out what installed it."
               :ex (str (p/green "shan") " remove emacs")}]
   :runs cli-remove
   :opts [opts/check? opts/temporary?]})
