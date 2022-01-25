(ns shan.commands.link
  (:require
   [shan.util :as u]
   [shan.utils.symlinks :as links]))

(defn- cli-link [{:keys [_arguments]}]
  (let [links (->> _arguments
                   (partition 2)
                   (reduce (fn [a [src dest]] (assoc a dest src)) {}))
        new-conf (update (u/get-new) :links merge links)]

    (links/create-links links)
    (u/write-to-conf new-conf)))

(def command
  {:command "link"
   :short "ln"
   :category "Linking"
   :arguments *
   :description "Creates symbolic links."
   :runs cli-link})
