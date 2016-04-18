(ns remlok.query
  (:require
    [schema.core :as s]))

;; TODO validation can't tell vector from list and accepts either in any cases
;; TODO Attr allows multiple values, which it shouldn't
;; TODO use custom validators

(declare Query)

(def Plain-Node
  s/Keyword)

(def Comp-Node
  {Plain-Node (s/recursive #'Query)})

(def Node
  (s/cond-pre Plain-Node Comp-Node))

(def Par-Node
  [(s/one Node "attr") (s/one s/Any "args")])

(def Fun-Node
  [(s/one s/Symbol "fun") (s/one Node "attr") (s/one s/Any "args")])

(def Query
  [(s/cond-pre
     Node
     Par-Node
     Fun-Node)])

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

(defn nodes [query]
  query)

(defn attr? [node]
  (keyword? node))

(defn pattr? [node]
  (and
    (list? node)
    (= (count node) 2)))

(defn join? [node]
  (map? node))

(defn call? [node]
  (and
    (list? node)
    (= (count node) 3)))

(defn attr [node]
  (cond
    (attr? node) node
    (pattr? node) (recur (first node))
    (join? node) (first (first node))
    (call? node) (recur (second node))
    :else nil))

(defn args [node]
  (cond
    (pattr? node) (second node)
    (call? node) (nth node 3)
    :else nil))

(defn subq [node]
  (cond
    (join? node) (second (first node))
    :else nil))