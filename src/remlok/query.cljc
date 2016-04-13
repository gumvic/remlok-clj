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
    (reduce
      (fn [res {:keys [attr] :as ast}]
        (if-let [{:keys [loc rem]} (execf ctx ast)]
          (-> res
              (assoc-in [:loc attr] loc)
              (update :rem (fn [r r*] (if r* (vec (concat r r*)) r)) rem))
          res))
      nil
      asts)))

(defn route [_ ast]
  (get ast :attr))

(comment
  (require '[remlok.query :refer [execute route]])
  (defmulti execf route)
  (defmethod execf :default [_ _]
    nil)
  (defmethod execf :foo [_ _]
    {:loc :foo})
  (defmethod execf :bar [_ _]
    {:loc :bar
     :rem [:bar]})
  (defmethod execf :baz [ctx {:keys [query]}]
    (let [{:keys [loc rem]} (execute execf ctx query)
          rem (when rem [{:baz rem}])]
      {:loc loc :rem rem}))
  (execute execf nil [:foo])
  (execute execf nil [:foo :bar :baz])
  (execute execf nil [{:baz [:foo :bar :zooz]}]))