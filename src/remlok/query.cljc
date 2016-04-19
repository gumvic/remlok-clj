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

#_(def AST
  [{(s/required-key :attr) s/Keyword
    (s/optional-key :fun) s/Symbol
    (s/optional-key :args) s/Any
    (s/optional-key :query) (s/recursive #'AST)}])

#_(declare query->ast)

#_(defn- attr->ast* [attr]
  (cond
    (keyword? attr)
    {:attr attr}
    (map? attr)
    (let [[attr query] (first attr)]
      {:attr attr
       :query (query->ast query)})))

#_(defn- attr->ast [attr]
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

#_(defn query->ast [query]
  (mapv attr->ast query))

#_(declare ast->query)

#_(defn- ast->attr* [ast]
  (let [{:keys [attr query]} ast]
    (if query
      {attr (ast->query query)}
      attr)))

#_(defn- ast->attr [ast]
  (let [{:keys [fun args]} ast]
    (let [attr (ast->attr* ast)]
      (cond
        fun `(~fun ~attr ~args)
        args `(~attr ~args)
        :else attr))))

#_(defn ast->query [ast]
  (mapv ast->attr ast))

(defn- attr->ast [attr]
  (cond
    (keyword? attr) {:attr attr}
    (map? attr) (let [[attr join] (first attr)]
                  {:attr attr
                   :join join})
    :else nil))

(defn node->ast [node]
  (cond
    (and
      (list? node) (= (count node) 2))
    (let [[attr args] node]
      (merge
        (attr->ast attr)
        {:args args}))
    (and
      (list? node) (= (count node) 3))
    (let [[fun attr args] node]
      (merge
        (attr->ast attr)
        {:fun fun
         :args args}))
    :else (attr->ast node)))

(defn- ast->attr [ast]
  (let [{:keys [attr join]} ast]
    (if join
      {attr join}
      attr)))

(defn ast->node [ast]
  (let [{:keys [fun args]} ast
        attr (ast->attr ast)]
    (cond
      fun `(~fun ~attr ~args)
      args `(~attr ~args)
      :else attr)))

#_(defn nodes [query]
  query)

#_(defn- attr* [node]
  (cond
    (keyword? node) node
    (map? node) (first (first node))
    :else nil))

#_(defn attr [node]
  (cond
    (and
      (list? node) (= (count node) 2))
    (attr* (first node))
    (and
      (list? node) (= (count node) 3))
    (attr* (second node))
    :else nil))

#_(defn join* [node]
  (cond
    (map? node) (second (first node))
    :else nil))

#_(defn join [node]
  (cond
    (and
      (list? node) (= (count node) 2))
    (join* (first node))
    (and
      (list? node) (= (count node) 3))
    (join* (second node))
    :else nil))

#_(defn fun [node]
  (cond
    (and
      (list? node) (= (count node) 3))
    (first node)
    :else nil))

#_(defn args [node]
  (cond
    (and
      (list? node) (= (count node) 2))
    (second node)
    (and
      (list? node) (= (count node) 3))
    (nth node 3)
    :else nil))

#_(defn attr? [node]
  (keyword? node))

#_(defn pattr? [node]
  (and
    (list? node)
    (= (count node) 2)))

#_(defn join? [node]
  (map? node))

#_(defn call? [node]
  (and
    (list? node)
    (= (count node) 3)))

#_(defn attr [node]
  (cond
    (attr? node) node
    (pattr? node) (recur (first node))
    (join? node) (first (first node))
    (call? node) (recur (second node))
    :else nil))

#_(defn args [node]
  (cond
    (pattr? node) (second node)
    (call? node) (nth node 3)
    :else nil))

#_(defn join [node]
  (cond
    (join? node) (second (first node))
    :else nil))

#_(defn fun [node]
  (cond
    (call? node) (first node)
    :else nil))