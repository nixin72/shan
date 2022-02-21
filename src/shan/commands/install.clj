(ns shan.commands.install
  (:require
   [shan.commands.-options :as opts]
   [shan.print :as p]
   [shan.util :as u]
   [shan.managers :as pm]
   [shan.packages :as ps]
   [shan.config :as c]
   [shan.cache :as cache]))

(defn generate-success-report
  "Used to check which package installations succeeded or failed.
  Returns a map of successful installations, the commands used for those
  installations, and a boolean to indicate if there were failures."
  [result]
  (reduce-kv (fn [a k v]
               (let [{:keys [s f c]}
                     (reduce #(-> (if (second %2)
                                    (update % :s conj (second %2))
                                    (update % :f conj (second %2)))
                                  (update :c conj (first %2)))
                             {:s [] :f [] :c []}
                             v)]
                 (-> (assoc-in a [:success k] s)
                     (update :commands into c)
                     (assoc :failed (boolean (seq f))))))
             {:success {} :commands #{} :failed false}
             result))

(defn find-default-manager
  "Returns a package map"
  [install-map]
  (if-let [pkgs (get install-map nil)]
    (let [[pm set-default?] (pm/determine-default-manager)]
      (-> install-map
          (dissoc nil)
          (assoc pm pkgs)
          ((fn [x]
             (if set-default? (assoc x :default-manager pm) x)))))
    install-map))

(defn cli-install
  "Tries to install all packages specified in _arguments using the correct package manager"
  [{:keys [check temp batch _arguments]}]
  (let [new-conf (u/get-new)
        install-map (u/flat-map->map _arguments (:default-manager new-conf))
        install-map (find-default-manager install-map)
        result (reduce-kv #(assoc %1 %2 (ps/install-pkgs %2 %3 check batch))
                          {}
                          (dissoc install-map :default-manager))
        {:keys [success failed commands]} (generate-success-report result)
        success (ps/replace-keys success)]
    (when failed
      (p/error "\nPackages that failed to install were not added to your config."))

    (cache/add-to-cache (ps/replace-keys install-map))
    (cond
      temp (u/add-to-temp success)
      (:default-manager install-map)
      (u/add-to-conf
       (assoc new-conf :default-manager (:default-manager install-map))
       success)
      :else (u/add-to-conf new-conf success))
    (if c/testing? commands p/exit-code)))

(def command
  {:command "install"
   :short "in"
   :category "Managing Packages"
   :arguments? *
   :description "Install packages through any supported package manager"
   :examples [{:desc (str "Set the default package manager to " (p/blue "yay"))
               :ex (str (p/green "shan") " default " (p/blue "yay"))}
              {:desc (str "Install packages through " (p/blue "yay") ", this config's default package manager")
               :ex (str (p/green "shan") " install open-jdk vscode")}
              {:desc "Install packages through a specified package manager"
               :ex (str (p/green "shan") " install " (p/blue "--npm") " react-native expo")}
              {:desc "Install packages through various package managers"
               :ex (str (p/green "shan") " install open-jdk vscode " (p/blue "--npm") " react-native expo " (p/blue "--pip") " PyYAML")}]
   :runs cli-install
   :opts [opts/check? opts/temporary? opts/batch?]})
