(ns remlok.playground.counter
  (:require
    [remlok.loc :refer [pub sub]]))

(pub
  :counter
  (fn [{:keys [db]} _]
    @db))

(defn root []
  (let [{:keys [counter]} @(sub [:counter])]
    (fn []
      [:span
       {:on-click #()}
       "Click here: " (str counter)])))