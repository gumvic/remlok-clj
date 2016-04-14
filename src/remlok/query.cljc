(ns remlok.query
  (:refer-clojure :exclude [compile])
  (:require
    [schema.core :as s]))

;; TODO validation can't tell vector from list and accepts either in any cases
;; TODO Attr allows multiple values, which it shouldn't
;; TODO use custom validators

(declare Query)

(def Plain-Attr
  s/Keyword)

(def Comp-Attr
  {Plain-Attr (s/recursive #'Query)})

(def Attr
  (s/cond-pre Plain-Attr Comp-Attr))

(def Par-Attr
  [(s/one Attr "attr") (s/one s/Any "args")])

(def Fun-Attr
  [(s/one s/Symbol "fun") (s/one Attr "attr") (s/one s/Any "args")])

(def Query
  [(s/cond-pre
     Attr
     Par-Attr
     Fun-Attr)])

(def AST
  [{(s/required-key :attr) s/Keyword
    (s/optional-key :fun) s/Symbol
    (s/optional-key :args) s/Any
    (s/optional-key :query) (s/recursive #'AST)}])