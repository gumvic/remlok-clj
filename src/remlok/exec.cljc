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
  (mapcat
    #(get (f ctx %) :loc)
    query))

(defn- mut-rem [f ctx query rem]
  (not-empty
    (into
      []
      (comp
        (map #(get (f ctx %) rem))
        (filter some?))
      query)))

(defn mut
  ([f ctx query]
   (mut f ctx query nil))
  ([f ctx query rem]
   (if-not rem
     (mut-loc f ctx query)
     (mut-rem f ctx query rem))))