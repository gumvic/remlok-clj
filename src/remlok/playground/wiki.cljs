(ns remlok.playground.wiki
  (:import [goog Uri]
           [goog.net Jsonp])
  (:require
    [reagent.ratom :refer-macros [reaction]]
    [remlok.loc :refer [pub sub mut mut! syncf]]
    [remlok.query :as q]))

(defn wiki [s res]
  (let [uri "http://en.wikipedia.org/w/api.php?action=opensearch&format=json&search="
        gjsonp (Jsonp. (Uri. (str uri s)))]
    (.send gjsonp nil (comp res second))))

(pub
  :search
  (fn [db]
    {:loc (reaction
            (get @db :search))}))

(pub
  :sugg
  (fn [db node]
    (let [s (q/args node)]
      {:loc
       (reaction
         (get-in @db [:sugg s]))
       :rem
       (when
         (and
           (> (count s) 2)
           (not (get-in @db [:sugg s])))
         `(:sugg ~s))})))

(mut
  :search
  (fn [db node]
    (let [s (q/args node)]
      {:loc (assoc db :search s)})))

(syncf
  (fn [req res]
    (let [s (-> (get req :subs)
                first
                first
                q/ast
                :args)]
      (wiki s #(res {:sugg {s %}})))))

(defn input []
  (let [props (sub [:search])]
    (fn []
      (let [{:keys [search]} @props]
        [:input
         {:on-change #(mut! `(:search ~(-> % .-target .-value)))
          :value (str search)}]))))

(defn list []
  (let [search (sub [:search])
        props (reaction
                (let [{:keys [search]} @search]
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