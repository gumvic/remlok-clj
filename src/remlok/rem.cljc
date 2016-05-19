(ns remlok.rem
  (:refer-clojure :exclude [read])
  (:require
    [remlok.impl :refer [select-fun]]))

;;;;;;;;;;;;;;;
;; Pub / Sub ;;
;;;;;;;;;;;;;;;

;; TODO emit cross-platform warning
(defn pubf
  "Default publication function.
  Doesn't do anything."
  [_ _])

;; TODO emit cross-platform warning
(defn mutf
  "Default mutation function.
  Doesn't do anything."
  [_ _])

(def ^:private pubs
  (atom
    {:remlok/default pubf}))

(def ^:private muts
  (atom
    {:remlok/default mutf}))

(defn pub
  "Publishes the topic using the supplied function.
  The function must be (ctx, query) -> any"
  [attr f]
  (swap! pubs assoc attr f))

(defn mut
  "Sets the mutation handler for the topic using the supplied function.
  The function must be (ctx, query) -> any"
  [attr f]
  (swap! muts assoc attr f))

(defn read
  "Reads the query using the function set by pub.
  Will fallback to the default (pubf) if no function is found for the topic."
  [ctx query]
  (let [f (select-fun @pubs query)]
    (f ctx query)))

(defn mut!
  "Performs mutation using the function set by mut.
  Will fallback to the default (mutf) if no function is found for the topic."
  [ctx query]
  (let [f (select-fun @muts query)]
    (f ctx query)))