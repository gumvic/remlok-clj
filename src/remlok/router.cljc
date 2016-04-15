(ns remlok.router
  (:refer-clojure :exclude [read]))

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