(ns remlok.exec
  (:require
    [remlok.query :as q]))

(defn- read-loc [f ctx query]
  (not-empty
    (into
      {}
      (comp
        (map #(when-let [val (get (f ctx %) :loc)]
               [(get (q/node->ast %) :attr) val]))
        (filter some?))
      query)))

(defn- read-rem [f ctx query rem]
  (not-empty
    (into
      []
      (comp
        (map #(get (f ctx %) rem))
        (filter some?))
      query)))

(defn read
  ([f ctx query]
    (read f ctx query nil))
  ([f ctx query rem]
   (if-not rem
     (let [read* #(read-loc f %1 %2)
           ctx* (assoc ctx :read read*)]
       (read* ctx* query))
     (let [read* #(read-rem f %1 %2 rem)
           ctx* (assoc ctx :read read*)]
       (read* ctx* query)))))

(defn- mut-loc [f ctx query]
  (let [loc (filter
              some?
              (map #(get (f ctx %) :loc) query))
        attrs (apply
                concat
                (into [] (comp (map :attrs) (filter some?)) loc))
        actions (into [] (comp (map :actions) (filter some?)) loc)]
    #(doseq [action actions]
      (action)
      attrs)))

(defn- mut-rem [f ctx query rem]
  (not-empty
    (into
      []
      (comp
        (map #(get (f ctx %) rem))
        (filter some?))
      query)))

(defn mut!
  ([f ctx query]
   (mut! f ctx query nil))
  ([f ctx query rem]
   (if-not rem
     (let [action (mut-loc f ctx query)]
       (action))
     (mut-rem f ctx query rem))))