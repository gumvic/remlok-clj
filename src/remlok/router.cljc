(ns remlok.router
  (:refer-clojure :exclude [read]))

(defn- attr->ast* [attr]
  (cond
    (keyword? attr) {:attr attr}
    (map? attr) {:attr (first (first attr))
                 :query (second (first attr))}))

(defn- attr->ast [attr]
  (if (list? attr)
    (if (= (count attr) 2)
      (let [[attr args] attr]
        (assoc
          (attr->ast* attr)
          :args args))
      (let [[fun attr args] attr]
        (assoc
          (attr->ast* attr)
          :fun fun
          :args args)))
    (attr->ast* attr)))

(defn- read-loc [readf ctx query]
  (not-empty
    (into
      {}
      (comp
        (map attr->ast)
        (map #(when-let [r (get (readf ctx %) :loc)]
               [(get % :attr) r]))
        (filter some?))
      query)))

(defn- read-rem [readf ctx query]
  (not-empty
    (into
      []
      (comp
        (map attr->ast)
        (map #(get (readf ctx %) :rem))
        (filter some?))
      query)))

(defn read [readf ctx query rem?]
  (if rem?
    (read-rem
      readf
      (assoc ctx
        :rem? true
        :rec #(read-rem readf %1 %2))
      query)
    (read-loc
      readf
      (assoc ctx
        :rem? false
        :rec #(read-loc readf %1 %2))
      query)))

(defn route [_ ast]
  (get :attr ast))