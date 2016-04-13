(ns remlok.router
  (:refer-clojure :exclude [read]))

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

(defn- step [readf ctx res ast]
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

(defn read [readf ctx query]
  (let [asts (map attr->ast query)]
    (reduce #(step readf ctx %1 %2) nil asts)))

(defn route [_ ast]
  (get ast :attr))