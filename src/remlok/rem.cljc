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

(defn pubf [])

(defn mutf [])

(def ^:private pubs
  (atom
    {:default pubf}))

(def ^:private muts
  (atom
    {:default mutf}))

(defn pub [attr f]
  (swap! pubs assoc attr f))

(defn read [ctx query]
  (let [f (select-fun @pubs query)]
    (f ctx query)))

(defn mut [attr f]
  (swap! muts assoc attr f))

(defn mut! [ctx query]
  (let [f (select-fun @muts query)]
    (f ctx query)))