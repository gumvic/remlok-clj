(ns remlok.query
  (:refer-clojure :exclude [compile])
  (:require
    [schema.core :as s]))

;; TODO validation can't tell vector from list and accepts either in any cases
;; TODO Comp-Attr allows multiple values, which it shouldn't
;; TODO use custom validators

(declare Query)

(def Plain-Attr
  s/Keyword)

(def Par-Attr
  [(s/one Plain-Attr "attr") (s/one s/Any "args")])

(def Comp-Attr
  {(s/cond-pre Plain-Attr Par-Attr)
   (s/recursive #'Query)})

(def Attr
  (s/cond-pre Plain-Attr Par-Attr Comp-Attr))

(def Query
  [Attr])

(def AST
  [{(s/required-key :attr) Attr
    (s/optional-key :args) s/Any
    (s/optional-key :query) (s/recursive #'AST)}])

(declare compile)

;; TODO [minor] refactor
(defn- compile* [attr]
  (cond
    (keyword? attr)
    {:attr attr}
    (list? attr)
    {:attr (first attr) :args (second attr)}
    (map? attr)
    (let [[a q] (first attr)]
      (cond
        (keyword? a)
        {:attr a :query (compile q)}
        (list? a)
        {:attr (first a) :args (second a) :query (compile q)}))))

(defn compile [query]
  (vec
    (map compile* query)))