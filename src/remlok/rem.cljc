(ns remlok.rem
  (:refer-clojure :exclude [read])
  (:require
    [remlok.impl :refer [handlers handle handler]]))

;;;;;;;;;;;;;;;
;; Pub / Sub ;;
;;;;;;;;;;;;;;;

(defn pubf
  "Default publication function.
  Doesn't do anything."
  [_ _])

(defn mutf
  "Default mutation function.
  Doesn't do anything."
  [_ _])

(def ^:private pubs
  (atom
    (handlers identity pubf)))

(def ^:private muts
  (atom
    (handlers identity mutf)))

(defn pub
  "Publishes the topic using the supplied function.
  The function must be (ctx, query) -> any
  Use :remlok/default topic to set the fallback."
  [topic f]
  (swap! pubs handle topic f))

(defn mut
  "Sets the mutation handler for the topic using the supplied function.
  The function must be (ctx, query) -> any
  Use :remlok/default topic to set the fallback."
  [topic f]
  (swap! muts handle topic f))

(defn read
  "Reads the query using the function set by pub."
  [ctx query]
  (let [[topic _] query
        f (handler @pubs topic)]
    (f ctx query)))

(defn mut!
  "Performs mutation using the function set by mut."
  [ctx query]
  (let [[topic _] query
        f (handler @muts topic)]
    (f ctx query)))