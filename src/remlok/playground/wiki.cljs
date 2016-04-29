(ns remlok.playground.wiki
  (:import [goog Uri]
           [goog.net Jsonp])
  (:require
    [remlok.loc :refer [pub sub rpub rsub mut mut! syncf]]
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

(syncf
  (fn [req res]
    (println req)))

(defn input [search]
  [:input
   {:on-change #(mut! `[(:search ~(-> % .-target .-value))])
    :value (str search)}])

(defn list [search]
  (let [props (sub `[(:sugg ~search)])]
    (fn []
      (let [{:keys [sugg]} @props]
        [:ul
         (map
           (fn [s]
             [:li (str s)])
           sugg)]))))

(defn root []
  (let [props (sub [:search])]
    (fn []
      (let [{:keys [search]} @props]
        [:div
         [input search]
         [list search]]))))