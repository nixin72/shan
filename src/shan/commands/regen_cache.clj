(ns shan.commands.regen-cache
  (:require
   [shan.cache :as cache]
   [shan.print :as p]))

(defn- cli-regen-cache [{:keys [_arguments]}]
  (cache/generate-cache))

(def command
  {:command "regen-cache"
   :short "rc"
   :arguments? 0
   :description "Regenerates the package cache"
   :examples [(str "Useful after using other package managers.\n\n"
                   "Checking every package manager to know where a package is installed"
                   " is very slow, so shan maintains a list of every package installed"
                   " on your system. Every operation performed with shan updates this cache -"
                   " however when using other package managers directly, it can get"
                   " out of date. While shan will periodically regenerate the cache in the background,"
                   " this approach can result in shan failing to uninstall packages"
                   " from time-to-time.\n\n"
                   "Regenerating the cache manually will make shan aware of any changes to the"
                   " system performed by other tools.\n\n")

              {:desc "Installs a package through npm - Shan has no way of knowing this."
               :ex (str (p/green "npm") " install react-native-cli")}
              {:desc "Trying to uninstall this through shan now may result in an error."
               :ex (str (p/green "shan") " remove react-native-cli")}
              {:desc "To ensure that shan is aware of the change, you regenerate the cache"
               :ex (str (p/green "shan") " regen-cache")}
              {:desc "And now uninstalling will work fine."
               :ex (str (p/green "shan") " remove react-native-cli")}]
   :runs cli-regen-cache})
