(ns remlok.playground.wiki
  (:import [goog Uri]
           [goog.net Jsonp])
  (:require
    [remlok.loc :refer [pub sub rpub rsub mut mut!]]
    [remlok.query :as q]))

(defn wiki [s res]
  (let [uri "http://en.wikipedia.org/w/api.php?action=opensearch&format=json&search="
        gjsonp (Jsonp. (Uri. (str uri s)))]
    (.send gjsonp nil (comp res second))))

(pub
  :search
  (fn [db]
    (fn []
      (get @db :search))))

(pub
  :sugg
  (fn [db]
    (fn []
      (get @db :sugg))))

(rpub
  :sugg
  (fn [db node]
    node))

(mut
  :search
  (fn [db node]
    (let [args (get (q/node->ast node) :args)]
      (assoc db :search args))))

(defn input [search]
  [:input
   {:on-change #(mut! `[(:search ~(-> % .-target .-value))])
    :value (str search)}])

(defn list [sugg]
  [:ul
   (map
     (fn [s]
       [:li (str s)])
     sugg)])

(defn root []
  (let [search (sub [:search])
        sugg ()]
    (fn []
      (let [{:keys [search sugg]} @props]
        [:div
         [input search]
         [list sugg]]))))