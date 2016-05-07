(ns remlok.playground.counter
  (:require
    [remlok.loc :refer [pub sub mut mut!]]))

(pub :counter (fn [db] db))
(mut :dec dec)
(mut :inc inc)

(defn root []
  (let [props (sub [:counter])]
    (fn []
      [:div
       [:button {:on-click #(mut! [:dec :dec])} "--"]
       [:button {:on-click #(mut! [:dec])} "-"]
       [:span (str (get @props :counter))]
       [:button {:on-click #(mut! [:inc])} "+"]
       [:button {:on-click #(mut! [:inc :inc])} "++"]])))