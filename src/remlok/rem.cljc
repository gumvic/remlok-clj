(ns remlok.rem
  (:refer-clojure :exclude [read])
  (:require
    [remlok.query :as q]))

;;;;;;;;;;;;;
;; Helpers ;;
;;;;;;;;;;;;;

(defn- select-fun [fs query]
  (get fs (q/topic query) (get fs :default)))

;;;;;;;;;;
;; Sync ;;
;;;;;;;;;;

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

(defn read [query]
  (let [f (select-fun @pubs query)]
    (f query)))

(defn mut [attr f]
  (swap! muts assoc attr f))

(defn mut! [query]
  (let [f (select-fun @muts query)]
    (f query)))