(ns remlok.rem
  (:refer-clojure :exclude [read]))

;;;;;;;;;;;;;
;; Helpers ;;
;;;;;;;;;;;;;

(defn- select-fun [fs [topic _]]
  (get fs topic (get fs :default)))

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