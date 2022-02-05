(ns shan.cache
  (:require
   [shan.print :as p]
   [shan.managers :as pm]
   [shan.config :as c]
   [shan.util :as u]))

(def package-cache (atom nil))

(defn- generate-cache []
  (p/logln "Updating cache")
  (let [pms (pm/installed-managers)
        cache (reduce-kv
               (fn [a k {:keys [list]}]
                 (let [pkgs (list :versions? false)]
                   (assoc a k pkgs)))
               {:regen-cache 8} pms)]
    (reset! package-cache cache)
    (u/write-edn c/cache-file cache)
    cache))

(defn read-cache []
  (if (nil? @package-cache)
    ;; Read the cache if it hasn't yet this run
    (let [cache (u/read-edn c/cache-file)]
      (if (nil? cache)
        ;; Generate a new cache if the cache is empty
        (generate-cache)
        (if (= (:regen-cache cache) 0)
          ;; Generate a new cache on another thread if it's "out of date" and return the old one
          (do (future (generate-cache))
              (reset! package-cache cache)
              cache)
          ;; Decrement the cache generation counter on another thread and return the current cache
          (do
            (future (u/write-edn c/cache-file (update cache :regen-cache dec)))
            (reset! package-cache cache)
            cache))))
    ;; Return the package cache that's already in-memory
    @package-cache))
