(ns remlok.playground.counter
  (:require
    [remlok.loc :refer [pub sub mut mut!]]))

;;;;;;;;;;;;;
;; Counter ;;
;;;;;;;;;;;;;
;; Features local subscriptions and mutations.

(pub :counter (fn [db] {:loc db}))
(mut :dec (fn [db] {:loc (dec db)}))
(mut :inc (fn [db] {:loc (inc db)}))

(defn root []
  (let [counter (sub [:counter])]
    (fn []
      [:div
       [:button {:on-click #(mut! [:dec])} "-"]
       [:span (str @counter)]
       [:button {:on-click #(mut! [:inc])} "+"]])))