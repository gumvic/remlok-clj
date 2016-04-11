(ns remlok.db)

(defn- wildcard? [x])

(defn- e-a-v [db e a v]
  (let [{:keys [eav aev ave vae]} @db]
    ))

(defn- e-a-* [db e a v]
  (let [{:keys [eav aev ave vae]} @db]
    ))

(defn- e-*-v [db e a v]
  (let [{:keys [eav aev ave vae]} @db]
    ))

(defn- e-*-* [db e a v]
  (let [{:keys [eav aev ave vae]} @db]
    ))

(defn- *-a-v [db e a v]
  (let [{:keys [eav aev ave vae]} @db]
    ))

(defn- *-a-* [db e a v]
  (let [{:keys [eav aev ave vae]} @db]
    ))

(defn- *-*-v [db e a v]
  (let [{:keys [eav aev ave vae]} @db]
    ))

(defn- *-*-* [db e a v]
  (let [{:keys [eav aev ave vae]} @db]
    ))

(defn read [db query]
  (let [[e a v] query]
    (case [(wildcard? e)
           (wildcard? a)
           (wildcard? v)]
      [false false false] (e-a-v db e a v)
      [false false true] (e-a-* db e a v)
      [false true false] (e-*-v db e a v)
      [false true true] (e-*-* db e a v)
      [true false false] (*-a-v db e a v)
      [true false true] (*-a-* db e a v)
      [true true false] (*-*-v db e a v)
      [true true true] (*-*-* db e a v))))

(defn mut! [db query]
  (let [{:keys [eav aev ave vae]} @db
        [e a v] query]))