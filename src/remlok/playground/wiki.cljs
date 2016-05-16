(ns remlok.playground.wiki
  (:import [goog Uri]
           [goog.net Jsonp])
  (:require
    [reagent.ratom :refer-macros [reaction]]
    [remlok.loc :as l]
    [remlok.rem :as r]
    [remlok.query :as q]
    [cljs.core.async :as a :refer [chan put! take!]]))

(defn wiki [s]
  (let [uri "http://en.wikipedia.org/w/api.php?action=opensearch&format=json&search="
        gjsonp (Jsonp. (Uri. (str uri s)))
        ch (chan)]
    (.send gjsonp nil #(put! ch (second %)))
    ch))

#_(r/pub
  :sugg
  (fn [node]
    (let [s (q/args node)]
      (wiki s))))

#_(l/pub
  :search
  (fn [db]
    {:loc (reaction
            (get @db :search))}))

#_(l/pub
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

(l/mut
  :search
  (fn [db query]
    (let [s (q/args query)]
      {:loc (assoc db :search s)})))

#_(l/sync
  (fn [{:keys [reads]} res]
    (let [reads* (a/map
                   (fn [& qr]
                     (into {} qr))
                   (for [q reads]
                     (a/map
                       (fn [& ar]
                         [q (into {} ar)])
                       (map
                         (fn [[a r]]
                           (let [ch (chan)]
                             (take! r #(put! ch [a %]))
                             ch))
                         (r/read q)))))]
      (take! reads* #(res {:reads %})))))

(defn input []
  (let [search (l/sub [:search])]
    (fn []
      [:input
       {:on-change #(l/mut! `[:search ~(-> % .-target .-value)])
        :value (str @search)}])))

(defn list []
  (let [search (l/sub [:search])
        props (reaction
                (let [{:keys [search]} @search]
                  @(l/sub `[(:sugg ~search)])))]
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