(ns shan.commands.unlink
  (:require [shan.util :as u]
            [shan.utils.symlinks :as links]))

(defn- cli-unlink [{:keys [_arguments]}]
  (let [links _arguments
        conf (update (u/get-new) :links #(apply dissoc % (map symbol links)))]

    (links/remove-links links)
    (u/write-to-conf conf)))

(def command
  {:command "unlink"
   :short "ul"
   :category "Linking"
   :arguments *
   :description "Removes symbolic links."
   :runs cli-unlink})
