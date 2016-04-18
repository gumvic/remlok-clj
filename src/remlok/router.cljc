(ns remlok.router
  (:require
    [remlok.query :as q]))

(defn route [_ node]
  (if-let [fun (q/fun node)]
    `(~fun ~(q/attr node))
    (q/attr node)))