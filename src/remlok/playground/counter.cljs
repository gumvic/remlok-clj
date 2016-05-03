(ns remlok.playground.counter
  (:require
    [remlok.router :refer [route]]
    [remlok.loc :refer [pub sub mut mut!]]))

(defmulti pubf route)
(defmethod pubf :counter [db]
  db)

(defmulti mutf route)
(defmethod mutf :inc [db]
  (inc db))
(defmethod mutf :dec [db]
  (dec db))

(pub pubf)
(mut mutf)

(defn root []
  (let [props (sub [:counter])]
    (fn []
      [:div
       [:button {:on-click #(mut! [:dec :dec])} "--"]
       [:button {:on-click #(mut! [:dec])} "-"]
       [:span (str (get @props :counter))]
       [:button {:on-click #(mut! [:inc])} "+"]
       [:button {:on-click #(mut! [:inc :inc])} "++"]])))