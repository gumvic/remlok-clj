(ns remlok.playground.counter
  (:require
    [remlok.router :refer [route]]
    [remlok.loc :refer [app ui mut!]]))

(defmulti readf route)
(defmethod readf :counter [{:keys [db]} _]
  (let [[_ _ i] (first
                  (db '[:st :counter _]))]
    {:loc (or i 0)}))
(defmethod readf :default [_ _]
  nil)

(defmulti mutf route)
(defmethod mutf :inc [_ _]
  {:loc
   [[:st :counter (fnil inc 0)]]})

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