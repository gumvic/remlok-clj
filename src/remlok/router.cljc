(ns remlok.router)

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

(defn read [readf ctx query]
  (let [asts (map attr->ast query)]
    (reduce
      (fn [res {:keys [attr] :as ast}]
        (if-let [{:keys [loc rem]} (readf ctx ast)]
          (-> res
              (assoc-in [:loc attr] loc)
              (update :rem (fn [r r*] (if r* (vec (concat r r*)) r)) rem))
          res))
      nil
      asts)))

(defn route [_ ast]
  (get ast :attr))