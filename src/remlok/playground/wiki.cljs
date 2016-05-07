(ns remlok.playground.wiki
  (:import [goog Uri]
           [goog.net Jsonp])
  (:require
    [reagent.ratom :refer-macros [reaction]]
    [remlok.loc :refer [pub sub rpub rsub mut mut! syncf]]
    [remlok.query :as q]))

(defn wiki [s res]
  (let [uri "http://en.wikipedia.org/w/api.php?action=opensearch&format=json&search="
        gjsonp (Jsonp. (Uri. (str uri s)))]
    (.send gjsonp nil (comp res second))))

(pub
  :search
  (fn [db]
    (reaction
      (get @db :search))))

(pub
  :sugg
  (fn [db node]
    (let [s (q/args node)]
      (reaction
        (get-in @db [:sugg s])))))

(rpub
  :sugg
  (fn [db node]
    (let [s (q/args node)]
      (when
        (and
          (> (count s) 2)
          (not (get-in db [:sugg s])))
        `(:sugg ~s)))))

(mut
  :search
  (fn [db node]
    (let [s (q/args node)]
      (assoc db :search s))))

(syncf
  (fn [req res]
    (let [s (-> (get req :subs)
                first
                first
                q/node->ast
                :args)]
      (wiki s #(res {:sugg {s %}})))))

(defn input []
  (let [props (sub [:search])]
    (fn []
      (let [{:keys [search]} @props]
        [:input
         {:on-change #(mut! `[(:search ~(-> % .-target .-value))])
          :value (str search)}]))))

(defn list []
  (let [props (reaction
                (let [{:keys [search]} @(sub [:search])]
                  @(sub `[(:sugg ~search)])))]
    (fn []
      (let [{:keys [sugg]} @props]
        [:ul
         (map
           (fn [s]
             [:li (str s)])
           sugg)]))))

(defn root []
  [:div
   [input]
   [list]])