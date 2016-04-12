(ns remlok.query
  (:refer-clojure :exclude [compile])
  (:require
    [schema.core :as s]))

;; TODO [minor] validation can't tell vector from list and accepts either in any cases

(declare Query)

(def Plain-Attr
  s/Keyword)

(def Par-Attr
  [(s/one Plain-Attr "attr") s/Any])

(def Comp-Attr
  {(s/cond-pre Plain-Attr Par-Attr)
   (s/recursive #'Query)})

(def Attr
  (s/cond-pre Plain-Attr Par-Attr Comp-Attr))

(def Query
  [Attr])

(def AST
  [{(s/required-key :attr) Attr
    (s/optional-key :args) [s/Any]
    (s/optional-key :query) (s/recursive #'AST)}])

(declare compile)

;; TODO [minor refactor]
(defn- compile* [ast attr]
  (cond
    (keyword? attr)
    (conj ast {:attr attr})
    (list? attr)
    (conj ast {:attr (first attr) :args (rest attr)})
    (map? attr)
    (into ast (for [[a q] attr
                    :let [q* (compile q)]]
                (cond
                  (keyword? a)
                  {:attr a :query q*}
                  (list? a)
                  {:attr (first a) :args (rest a) :query q*})))))

(defn compile [query]
  (reduce compile* [] query))