(ns remlok.router
  (:require
    [remlok.query :as q]))

(defn- route* [node]
  (-> node q/node->ast :attr))

(defn route
  ([_ node]
   (route* node))
  ([_ node _]
   (route* node)))