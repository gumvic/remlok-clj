(ns remlok.playground.counter
  (:require
    [remlok.loc :refer [pub sub hub msg]]
    [remlok.router :refer [route]]))

(defmulti res route)
(defmethod res :counter [{:keys [db]} _]
  @db)
(defmethod res :default [_ _]
  nil)

(defmulti trans route)
(defmethod trans :inc [db _]
  (inc db))
(defmethod trans :default [db _]
  db)

(pub res)
(hub trans)

(defn root []
  (let [counter (sub [:counter])]
    (fn []
      [:span
       {:on-click #(msg :inc)}
       "Click here: " (str (get @counter :counter))])))