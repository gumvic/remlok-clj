(ns remlok.playground.counter
  (:require
    [remlok.loc :refer [pub sub mut mut!]]))

(pub :counter (fn [db] {:loc db}))
(mut :dec (fn [db] {:loc (dec db)}))
(mut :inc (fn [db] {:loc (inc db)}))

(defn root []
  (let [props (sub [:counter])]
    (fn []
      [:div
       [:button {:on-click #(mut! :dec)} "-"]
       [:span (str (get @props :counter))]
       [:button {:on-click #(mut! :inc)} "+"]])))