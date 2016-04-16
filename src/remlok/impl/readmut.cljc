(ns remlok.impl.readmut)

(defn- read-loc [readf ctx ast]
  (let [read* #(read-loc readf %1 %2)
        ctx (assoc ctx :read read*)]
    (not-empty
      (into
        {}
        (comp
          (map #(when-let [r (get (readf ctx %) :loc)]
                 [(get % :attr) r]))
          (filter some?))
        ast))))

(defn- read-rem [readf ctx ast]
  (let [read* #(read-rem readf %1 %2)
        ctx (assoc ctx :read read*)]
    (not-empty
      (into
        []
        (comp
          (map #(get (readf ctx %) :rem))
          (filter some?))
        ast))))

(defn- mut-loc [mutf ctx ast]
  (let [mut* #(read-rem mutf %1 %2)
        ctx (assoc ctx :mut mut*)]
    (let [fs (into
               []
               (comp
                 (map #(get (mutf ctx %) :loc))
                 (filter some?))
               ast)]
      (fn []
        (doseq [f fs]
          (f))))))

(defn- mut-rem [mutf ctx ast]
  (let [mut* #(read-rem mutf %1 %2)
        ctx (assoc ctx :mut mut*)]
    (not-empty
      (into
        []
        (comp
          (map #(get (mutf ctx %) :rem))
          (filter some?))
        ast))))