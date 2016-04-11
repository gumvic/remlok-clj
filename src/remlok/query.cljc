(ns remlok.query
  (:require
    [schema.core :as s]))

;; TODO [minor] validation can't tell vector from list and accepts either in any cases

(declare Query)

(def Attr
  s/Keyword)

(def ParAttr
  [(s/one Attr "attr") s/Any])

(def CompAttr
  {(s/cond-pre Attr ParAttr)
   (s/recursive #'Query)})

(def Query
  [(s/cond-pre Attr ParAttr CompAttr)])

(def AST
  [{(s/required-key :attr) Attr
    (s/optional-key :args) [s/Any]
    (s/optional-key :query) (s/recursive #'AST)}])