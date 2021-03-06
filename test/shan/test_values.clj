(ns shan.test-values
  (:require
   [clojure.java.shell :as sh]
   [shan.util :as u]
   [flatland.ordered.map :refer [ordered-map]]
   [flatland.ordered.set :refer [ordered-set]]))

(def ^:dynamic verbose? false)

(def default-package-manager
  (case (System/getProperty "os.name")
    "Linux" (case (:out (sh/sh "lsb_release" "-i" "-s"))
              ("ManjaroLinux\n" "arch\n") :yay
              ("Ubuntu") :apt
              ("Fedora") :dnf
              ("centos") :yum
              nil)
    "Mac OS X" :brew
    :choco))

(def pre-installed-packages
  '{:pacman #{atop emacs firefox make man-db neovim readline unzip}
    :npm #{atop is-odd}
    :pip #{wakatime thefuck}
    :gem #{csv etc json openssl readline uri yaml}})

(def sample-config (ordered-map {:default-manager default-package-manager
                                 :default (ordered-set '[neofetch])
                                 :npm (ordered-set '[underscore])
                                 :pip (ordered-set '[thefuck])}))

(def dpm (str default-package-manager))
(def empty-input [])
(def simple-input ["fzf"])
(def multiple-input ["fzf" "neofetch"])
(def multiple-pm-input ["fzf" "neofetch" ":npm" "react"])
(def several-pm-input ["fzf" "neofetch" ":npm" "react" ":pip" "thefuck" ":gem" "yaml"])
(def same-pm-twice-input ["fzf" "neofetch" ":npm" "react" dpm "atop" ":npm" "expo"])

(def install-map-simple-input (u/flat-map->map simple-input nil))

(def complex-config
  '{:default-manager :yay
    :yay [micro nano]
    :npm [expo react]
    :pip [thefuck]})

(def deserialized-complex-config
  '{:default-manager :yay
    :yay #{micro nano}
    :npm #{expo react}
    :pip #{thefuck}})

(def duplicating-config
  '{:yay [micro nano atop htop readline]
    :npm [expo react atop]
    :pip [thefuck]
    :gem [readline]})

(def deserialized-duplicating-config
  '{:yay #{micro nano atop htop readline}
    :npm #{expo react atop}
    :pip #{thefuck}
    :gem #{readline}})

(def temporary-packages
  '{:yay [tldr]
    :npm [is-even]})

(def serialized-config
  (assoc complex-config
         :npm ["@react-navigation/stack"]))

(def deserialized-config
  (assoc deserialized-complex-config
         :npm #{"@react-navigation/stack"}))
