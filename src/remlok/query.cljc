(ns remlok.query
  (:refer-clojure :exclude [compile])
  (:require
    [schema.core :as s]))

;; TODO validation can't tell vector from list and accepts either in any cases
;; TODO Attr allows multiple values, which it shouldn't
;; TODO use custom validators

(declare Query)

(def Attr
  (s/cond-pre
    s/Keyword
    {s/Keyword (s/recursive #'Query)}
    [(s/one s/Keyword "attr")
     (s/one s/Any "args")]
    [(s/one {s/Keyword (s/recursive #'Query)} "attr")
     (s/one s/Any "args")]))

(def Query
  [Attr])

(def AST
  [{(s/required-key :attr) s/Keyword
    (s/optional-key :args) s/Any
    (s/optional-key :query) (s/recursive #'AST)}])