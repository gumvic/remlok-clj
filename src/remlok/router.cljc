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

#_(defn- step [readf ctx res ast]
  (if-let [res* (readf ctx ast)]
    (-> res
        (assoc-in
          [:loc (get ast :attr)]
          (get res* :loc))
        (update
          :rem
          #(if (seq %2)
            (vec (concat %1 %2))
            %1)
          (get res* :rem)))
    res))

#_(defn- step [readf ctx res ast]
  (if-let [res* (readf ctx ast)]
    (assoc res )
    res))

#_(defn read [readf ctx query]
  (let [asts (map attr->ast query)]
    (reduce #(step readf ctx %1 %2) nil asts)))

(defn read [readf ctx query]
  (into
    {}
    (comp
      (map attr->ast)
      (map #(when-let [r (readf ctx %)]
             [(get % :attr) r]))
      (filter some?))
    query))

(defn route [_ ast]
  (get ast :attr))