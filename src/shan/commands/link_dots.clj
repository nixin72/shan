(ns shan.commands.link-dots
  (:import [java.nio.file FileSystems])
  (:require
   [clojure.string :as str]
   [shan.print :as p]
   [shan.utils.paths :as paths]
   [shan.utils.symlinks :as links]
   [shan.util :as u :refer [suppress-stdout]]
   [shan.commands.-options :as opts]))

(defn- parse-shanignore [path]
  (->> (path path ".shanignore")
       (.toString)
       (slurp)
       (str/split-lines)
       (#(conj % ".git"))         ; Add .git so it's always ignored
       (map #(.getPathMatcher
              (FileSystems/getDefault) (str "glob:**/" %)))))

(defn- cli-link-dotfiles [{:keys [check _arguments]}]
  (let [[src dest] (map #(paths/path %) _arguments)
        ignore-files (parse-shanignore src)]
    (suppress-stdout
     (not check)
     (links/link-structures src dest ignore-files))
    (p/logln "Done.")))

(def command
  {:command "link-rec"
   :short "lr"
   :category "Linking"
   :arguments ["src" "dest"]
   :description "Creates symbolic links matching a directory structure"
   :desc-long
   ["\n Walks recursively through the src directory and for every file, symlinks it in the dest."
    "\n If a directory doesn't exist in the dest, the directory itself will be linked."]
   :runs cli-link-dotfiles
   :opts [opts/check?]})
