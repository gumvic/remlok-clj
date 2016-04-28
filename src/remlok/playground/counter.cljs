(ns remlok.playground.counter
  (:require
    [remlok.loc :refer [pub sub mut mut!]]))

(pub :counter deref)

(mut :inc inc)

(mut :dec dec)

(defn root []
  (let [counter (sub [:counter])]
    (fn []
      [:div
       [:button {:on-click #(mut! [:dec :dec])} "--"]
       [:button {:on-click #(mut! [:dec])} "-"]
       [:span (str (get @counter :counter))]
       [:button {:on-click #(mut! [:inc])} "+"]
       [:button {:on-click #(mut! [:inc :inc])} "++"]])))