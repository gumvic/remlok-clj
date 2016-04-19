(ns remlok.loc.impl.query
  (:require
    [clojure.zip :as zip]
    [remlok.query :as q]))

(defn- query-zip [query]
  (zip/zipper
    #(or (vector? %) (map? %) (list? %))
    seq
    (fn [node children]
      (cond
        (vector? node) (vec children)
        (map? node) (into {} children)
        (seq? node) children))
    query))

(defn- arg? [x]
  (and
    (symbol? x)
    (= (first (name x)) \?)))

(defn- arg->keyword [arg]
  (keyword
    (subs (name arg) 1)))

;; TODO only replace in args
(defn query+args [query args]
  (loop [loc (query-zip query)]
    (if (zip/end? loc)
      (zip/root loc)
      (let [node (zip/node loc)
            loc* (if (arg? node)
                   (zip/replace loc (get args (arg->keyword node)))
                   loc)]
        (recur (zip/next loc*))))))

(defn query->attrs [query]
  (distinct
    (reduce
      (fn [attrs node]
        (let [{:keys [attr join]} (q/node->ast node)]
          (into
            (conj attrs attr)
            (when join (query->attrs join)))))
      []
      query)))

(declare query+attrs)

(defn- node+attrs [node attrs]
  (let [{:keys [attr join] :as ast} (q/node->ast node)]
    (if (some #(= attr %) attrs)
      node
      (when join
        (when-let [join* (query+attrs join attrs)]
          (q/ast->node
            (assoc ast :join join*)))))))

(defn query+attrs [query attrs]
  (not-empty
    (into
      []
      (comp
        (map #(node+attrs % attrs))
        (filter some?))
      query)))