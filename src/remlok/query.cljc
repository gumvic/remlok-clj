(ns remlok.query
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

(declare query->ast)

(defn- attr->ast* [attr]
  (cond
    (keyword? attr)
    {:attr attr}
    (map? attr)
    (let [[attr query] (first attr)]
      {:attr attr
       :query (query->ast query)})))

(defn- attr->ast [attr]
  (if (list? attr)
    (cond
      (= (count attr) 2)
      (let [[attr args] attr]
        (assoc
          (attr->ast* attr)
          :args args))
      (= (count attr) 3)
      (let [[fun attr args] attr]
        (assoc
          (attr->ast* attr)
          :fun fun
          :args args)))
    (attr->ast* attr)))

(defn query->ast [query]
  (mapv attr->ast query))

(declare ast->query)

(defn- ast->attr* [ast]
  (let [{:keys [attr query]} ast]
    (if query
      {attr (ast->query query)}
      attr)))

(defn- ast->attr [ast]
  (let [{:keys [fun args]} ast]
    (let [attr (ast->attr* ast)]
      (cond
        fun `(~fun ~attr ~args)
        args `(~attr ~args)
        :else attr))))

(defn ast->query [ast]
  (mapv ast->attr ast))