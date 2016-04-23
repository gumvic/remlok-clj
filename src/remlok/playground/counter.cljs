(ns remlok.playground.counter
  (:require
    [remlok.router :refer [route]]
    [remlok.loc :refer [app ui mut!]]
    [remlok.loc.db :as db]))

(defmulti readf route)
(defmethod readf :counter [{:keys [db]} _]
  (let [[_ _ i] (db/read db '[:st :counter _])]
    {:loc (or i 0)}))
(defmethod readf :default [_ _]
  nil)

(defmulti mutf route)
(defmethod mutf :inc [_ _]
  {:loc
   {:mut (fn [db]
           (let [[_ _ i] (db/read db '[:st :counter _])
                 i* (if i (inc i) 0)
                 db* (db/mut db [:st :counter i*])]
             (println db*)
             db*))
    :attrs [:counter]}})

(def root
  (ui
    {:query [:counter]
     :render
     (fn [{:keys [counter]} ui]
       (println "Have I just rendered twice?")
       [:span
        {:on-click #(mut! ui [:inc])}
        (str counter)])}))

(def main
  (app
    {:readf readf
     :mutf mutf}))