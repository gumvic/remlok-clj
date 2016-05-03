(ns remlok.playground.wiki
  (:import [goog Uri]
           [goog.net Jsonp])
  (:require
    [reagent.ratom :refer-macros [reaction]]
    [remlok.router :refer [route]]
    [remlok.loc :refer [pub sub rpub rsub mut mut! syncf]]
    [remlok.query :as q]))

(defn wiki [s res]
  (let [uri "http://en.wikipedia.org/w/api.php?action=opensearch&format=json&search="
        gjsonp (Jsonp. (Uri. (str uri s)))]
    (.send gjsonp nil (comp res second))))

(defmulti pubf route)
(defmethod pubf :search [db]
  (reaction
    (get @db :search)))
(defmethod pubf :sugg [db node]
  (reaction
    (get-in @db [:sugg (-> node q/node->ast :args)])))

(defmulti mutf route)
(defmethod mutf :search [db node]
  (assoc db :search (-> node q/node->ast :args)))

(defmulti rpubf route)
(defmethod rpubf :sugg [db node]
  (let [s (-> node q/node->ast :args)]
    (when-not (get-in db [:sugg s])
      `(:sugg ~s))))
(defmethod rpubf :default []
  nil)

(pub pubf)
(mut mutf)
(rpub rpubf)
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