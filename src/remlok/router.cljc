(ns remlok.router
  (:refer-clojure :exclude [read]))

#_(defn- attr->ast* [attr]
  (cond
    (keyword? attr) {:attr attr}
    (map? attr) {:attr (first (first attr))
                 :query (second (first attr))}))

#_(defn- attr->ast [attr]
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

#_(defn- read-loc [readf ctx query]
  (not-empty
    (into
      {}
      (comp
        (map attr->ast)
        (map #(when-let [r (get (readf ctx %) :loc)]
               [(get % :attr) r]))
        (filter some?))
      query)))

#_(defn- read-rem [readf ctx query]
  (not-empty
    (into
      []
      (comp
        (map attr->ast)
        (map #(get (readf ctx %) :rem))
        (filter some?))
      query)))

(defn- read-loc [readf ctx ast]
  (not-empty
    (into
      {}
      (comp
        (map #(when-let [r (get (readf ctx %) :loc)]
               [(get % :attr) r]))
        (filter some?))
      ast)))

(defn- read-rem [readf ctx ast]
  (not-empty
    (into
      []
      (comp
        (map #(get (readf ctx %) :rem))
        (filter some?))
      ast)))

(defn read [readf ctx ast]
  {:loc (read-loc
          readf
          (assoc ctx :read (partial read-loc readf))
          ast)
   :rem (read-rem
          readf
          (assoc ctx :read (partial read-rem readf))
          ast)})

#_(defn- mut-loc [mutf ctx query]
  (let [fs (into
             []
             (comp
               (map #(get (mutf ctx %) :loc))
               (filter some?))
             query)]
    (fn []
      (doseq [f fs]
        (f)))))

#_(defn- mut-rem [mutf ctx query]
  (not-empty
    (into
      []
      (comp
        (map attr->ast)
        (map #(get (mutf ctx %) :rem))
        (filter some?))
      query)))

(defn- mut-loc [mutf ctx ast]
  (let [fs (into
             []
             (comp
               (map #(get (mutf ctx %) :loc))
               (filter some?))
             ast)]
    (fn []
      (doseq [f fs]
        (f)))))

(defn- mut-rem [mutf ctx ast]
  (not-empty
    (into
      []
      (comp
        (map #(get (mutf ctx %) :rem))
        (filter some?))
      ast)))

(defn mut [mutf ctx ast]
  {:loc (mut-loc
          mutf
          (assoc ctx :mut (partial mut-loc mutf))
          ast)
   :rem (mut-rem
          mutf
          (assoc ctx :mut (partial mut-rem mutf))
          ast)})

(defn route [_ ast]
  (if-let [fun (get ast :fun)]
    `(~fun ~(get ast :attr))
    (get ast :attr)))