(ns remlok.loc.db
  (:refer-clojure :exclude [read]))

(defn- wildcard? [x]
  (= x '_))

(defn- eav [db e a v]
  (let [{:keys [eav]} db]
    (for [v* (get-in eav [e a])
          :when (= v v*)]
      [e a v*])))

(defn- ea_ [db e a _]
  (let [{:keys [eav]} db]
    (for [v* (get-in eav [e a])]
      [e a v*])))

(defn- e_v [db e _ v]
  (let [{:keys [eav]} db
        av (get eav e)]
    (mapcat
      (fn [[a vs]]
        (for [v* vs
              :when (= v* v)]
          [e a v*]))
      av)))

(defn- e__ [db e _ _]
  (let [{:keys [eav]} db
        av (get eav e)]
    (mapcat
      (fn [[a vs]]
        (for [v* vs]
          [e a v*]))
      av)))

(defn- _av [db _ a v]
  (let [{:keys [ave]} db]
    (for [e* (get-in ave [a v])]
      [e* a v])))

(defn- _a_ [db _ a _]
  (let [{:keys [aev]} db
        ev (get aev a)]
    (mapcat
      (fn [[e vs]]
        (for [v* vs]
          [e a v*]))
      ev)))

(defn- __v [db _ _ v]
  (let [{:keys [vae]} db
        ae (get vae v)]
    (mapcat
      (fn [[a es]]
        (for [e* es]
          [e* a v]))
      ae)))

(defn- ___ [db _ _ _]
  (let [{:keys [eav]} db]
    (mapcat
      (fn [[e av]]
        (mapcat
          (fn [[a vs]]
            (for [v* vs]
              [e a v*]))
          av))
      eav)))

(defn- read* [db query]
  (let [[e a v] query]
    (case [(wildcard? e)
           (wildcard? a)
           (wildcard? v)]
      [false false false] (eav db e a v)
      [false false true] (ea_ db e a v)
      [false true false] (e_v db e a v)
      [false true true] (e__ db e a v)
      [true false false] (_av db e a v)
      [true false true] (_a_ db e a v)
      [true true false] (__v db e a v)
      [true true true] (___ db e a v)
      nil)))

(defn- mut* [db query]
  (let [{:keys [eav aev ave vae]} db
        [e a v] query
        eav* (update-in eav [e a] (fnil conj []) v)
        aev* (update-in aev [a e] (fnil conj []) v)
        ave* (update-in ave [a v] (fnil conj []) e)
        vae* (update-in vae [v a] (fnil conj []) e)]
    (assoc db :eav eav* :aev aev* :ave ave* :vae vae*)))

(defn read [st query]
  (read* (get st :remlok/db) query))

(defn mut! [st query]
  (vswap! st update :remlok/db mut* query))