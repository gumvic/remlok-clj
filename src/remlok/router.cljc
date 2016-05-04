(ns remlok.router
  (:require
    [remlok.query :as q]))

(defn- route* [node]
  (q/attr node))

(defn route
  ([_ node]
   (route* node))
  ([_ node _]
   (route* node)))