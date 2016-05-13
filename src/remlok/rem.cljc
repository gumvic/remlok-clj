(ns remlok.rem
  (:refer-clojure :exclude [read])
  (:require
    [remlok.query :as q]))

;;;;;;;;;;
;; Sync ;;
;;;;;;;;;;

(defn syncf [f])

;;;;;;;;;;;;;;;
;; Pub / Sub ;;
;;;;;;;;;;;;;;;

(def ^:private pubs
  (atom
    {:default (fn [])}))

(def ^:private muts
  (atom
    {:default (fn [])}))

(defn pub [attr f]
  (swap! pubs assoc attr f))

(defn- read* [node]
  (let [attr (q/attr node)
        df (get @pubs :default)
        f (get @pubs attr df)
        r (f node)]
    (when r
      [attr r])))

(defn read [query]
  (into
    {}
    (comp
      (map read*)
      (filter some?))
    query))

(defn mut [attr f]
  (swap! muts assoc attr f))

(defn mut! [node]
  (let [attr (q/attr node)
        df (get @muts :default)
        f (get @muts attr df)]
    (f node)))