(ns remlok.playground.counter
  (:require
    [remlok.router :refer [route]]
    [remlok.loc :refer [app ui]]))

(defmulti readf route)
(defmethod readf :counter [db _]
  db)
(defmethod readf :default [_ _]
  nil)

(def root
  (ui
    {:query [:counter]
     :render
     (fn [{:keys [counter]}]
       [:span (str counter)])}))

(def main
  (app
    {:db 0
     :readf readf}))