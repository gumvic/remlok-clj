(ns remlok.router)

(defn route [_ ast]
  (if-let [fun (get ast :fun)]
    `(~fun ~(get ast :attr))
    (get ast :attr)))