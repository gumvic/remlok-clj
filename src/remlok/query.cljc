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