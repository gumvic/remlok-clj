(ns remlok.query
  (:refer-clojure :exclude [compile])
  (:require
    [schema.core :as s]))

;; TODO validation can't tell vector from list and accepts either in any cases
;; TODO Comp-Attr allows multiple values, which it shouldn't
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

(defn- attr->ast* [attr]
  (cond
    (keyword? attr) {:attr attr}
    (map? attr) {:attr (first (first attr))
                 :query (second (first attr))}))

(defn- attr->ast [attr]
  (if (list? attr)
    (assoc
      (attr->ast* (first attr))
      :args (second attr))
    (attr->ast* attr)))

(defn execute [execf ctx query]
  (let [asts (map attr->ast query)]
    (into
      {}
      (fn [{:keys [attr] :as ast}]
        [attr (execf ctx ast)])
      asts)))

(defn route [_ ast]
  (get ast :attr))