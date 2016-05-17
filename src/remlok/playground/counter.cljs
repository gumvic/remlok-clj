(ns remlok.playground.counter
  (:require
    [remlok.loc :refer [pub read mut mut!]]))

;;;;;;;;;;;;;
;; Counter ;;
;;;;;;;;;;;;;
;; Features local subscriptions and mutations.

(pub :counter (fn [db] {:loc db}))
(mut :dec (fn [db] {:loc (dec db)}))
(mut :inc (fn [db] {:loc (inc db)}))

(defn root []
  (let [counter (read [:counter])]
    (fn []
      [:div
       [:button {:on-click #(mut! [:dec])} "-"]
       [:span (str @counter)]
       [:button {:on-click #(mut! [:inc])} "+"]])))