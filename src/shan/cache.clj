(ns shan.cache
  (:require
   [shan.print :as p]
   [shan.managers :as m]
   [shan.config :refer [app-config]]
   [shan.util :as u]))

(def package-cache (atom nil))
(def cache-lock (atom false))

(defn generate-cache []
  (p/logln "Updating cache")
  (when-not @cache-lock
    (reset! cache-lock true)
    (let [pms (m/installed-managers)
          cache (reduce-kv
                 (fn [a k {:keys [list uses]}]
                   (if (nil? uses)
                     (let [pkgs (list :versions? false)]
                       (assoc a k pkgs))
                     a))
                 {:regen-cache 4} pms)]
      (u/write-edn (:cache-file @app-config) cache)
      (reset! package-cache cache)
      (reset! cache-lock false)
      cache)))

(defn read-cache []
  (if (nil? @package-cache)
    ;; Read the cache if it hasn't yet this run
    (let [cache (u/read-edn (:cache-file @app-config))]
      (if (nil? cache)
        ;; Generate a new cache if the cache is empty
        (if-let [cache (generate-cache)]
          ;; Return the cache
          cache
          ;; If the cache is already in the middle of being updated, wait for the lock to be released
          (do (while @cache-lock (Thread/sleep 100))
              @package-cache))
        (if (= (:regen-cache cache) 0)
          ;; Generate a new cache on another thread if it's "out of date" and return the old one
          (do (future (generate-cache))
              (reset! package-cache cache)
              cache)
          ;; Decrement the cache generation counter on another thread and return the current cache
          (do
            (future (u/write-edn (:cache-file @app-config) (update cache :regen-cache dec)))
            (reset! package-cache cache)
            cache))))
    ;; Return the package cache that's already in-memory
    @package-cache))

(defn add-to-cache [pkgs]
  (while (or @cache-lock (nil? @package-cache)) nil)
  (let [updated-cache (swap! package-cache u/merge-conf pkgs)]
    (u/write-edn (:cache-file @app-config) updated-cache)
    updated-cache))

(defn remove-from-cache [pkgs]
  (while (or @cache-lock (nil? @package-cache)) nil)
  (let [updated-cache (swap! package-cache u/remove-from-config pkgs)]
    (u/write-edn (:cache-file @app-config) updated-cache)
    updated-cache))
