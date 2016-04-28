(ns remlok.router
  (:require
    [remlok.query :as q]))

(defn- res* [resf db query ctx]
  (not-empty
    (into
      {}
      (comp
        (map #(when-let [res (resf db % ctx)]
               [(get (q/node->ast %) :attr) res]))
        (filter some?))
      query)))

(defn route [_ node]
  (get (q/node->ast node) :attr))