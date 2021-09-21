(ns shan.config)

(def proj-name "shan")
(def home (System/getenv "HOME"))
(def gen-dir (str home "/.local/share/" proj-name))
(def gen-file (str gen-dir "/generations.edn"))
(def conf-file (str home "/.config/" proj-name ".edn"))
(def temp-file (str gen-dir "/temporary.edn"))
