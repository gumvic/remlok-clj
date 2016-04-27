(ns remlok.router
  (:require
    [remlok.query :as q]))

(defn route
  ([_ node]
   (get (q/node->ast node) :attr)))