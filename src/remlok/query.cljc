(ns remlok.query
  (:require
    [schema.core :as s]))

;; TODO schemas are for doc purposes and don't work properly

(declare Query)

(def Attr
  (s/cond-pre
    s/Keyword
    {(s/recursive #'Attr) (s/recursive #'Query)}))

(def Query
  [(s/cond-pre
     Attr
     [(s/one Attr "attr") (s/one s/Any "args")])])

(defn- attr->ast [attr]
  (cond
    (keyword? attr) {:attr attr}
    (map? attr) (let [[attr join] (first attr)]
                  {:attr attr
                   :join join})
    :else nil))

(defn ast [node]
  (cond
    (list? node)
    (let [[attr args] node]
      (merge
        (attr->ast attr)
        {:args args}))
    :else (attr->ast node)))

(defn- ast->attr [ast]
  (let [{:keys [attr join]} ast]
    (if join
      {attr join}
      attr)))

(defn node [ast]
  (let [{:keys [args]} ast
        attr (ast->attr ast)]
    (cond
      args `(~attr ~args)
      :else attr)))

(defn attr [node]
  (-> node ast :attr))

(defn args [node]
  (-> node ast :args))