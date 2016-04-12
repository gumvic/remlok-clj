(ns remlok.router)

(defn route [_ ast]
  (get ast :attr))