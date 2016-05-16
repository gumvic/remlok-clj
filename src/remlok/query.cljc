(ns remlok.query)

(defn query [topic args]
  [topic args])

(defn topic [query]
  (first query))

(defn args [query]
  (second query))