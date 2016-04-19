(ns remlok.playground.counter
  (:require
    [remlok.router :refer [route]]
    [remlok.loc :refer [app ui mut!]]))

(defmulti readf route)
(defmethod readf :counter [{:keys [db]} _]
  {:loc db})
(defmethod readf :default [_ _]
  nil)

(defmulti mutf route)
(defmethod mutf :inc [{:keys [db]} _]
  {:loc
   {:action inc
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
    {:db 0
     :readf readf
     :mutf mutf}))