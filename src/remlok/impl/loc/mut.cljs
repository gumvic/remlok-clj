(ns remlok.impl.loc.mut
  (:require
    [remlok.impl.loc.db :refer [db]]
    [remlok.impl.loc.sync :refer [sched-mut!]]
    [remlok.query :as q]))

(def ^:private muts
  (atom {}))

(defn mut [attr f]
  (swap! muts assoc-in [:loc attr] f))

(defn rmut [attr f]
  (swap! muts assoc-in [:rem attr] f))

(defn- mut** [db node]
  (let [attr (q/attr node)
        f (->>
            (fn [db] db)
            (get-in @muts [:loc :default])
            (get-in @muts [:loc attr]))]
    (f db node)))

(defn- mut* [db query]
  (reduce mut** db query))

(defn- rmut* [node]
  (let [attr (q/attr node)
        f (->>
            (constantly nil)
            (get-in @muts [:rem :default])
            (get-in @muts [:rem attr]))]
    (f (peek db) node)))

(defn- rmut [query]
  (into
    []
    (comp
      (map rmut*)
      (filter some?))
    query))

(defn mut! [query]
  (sched-mut!
    (rmut query))
  (swap! db mut* query)
  nil)