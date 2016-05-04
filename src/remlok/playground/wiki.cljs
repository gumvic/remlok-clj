(ns remlok.playground.wiki
  (:import [goog Uri]
           [goog.net Jsonp])
  (:require
    [reagent.ratom :refer-macros [reaction]]
    [remlok.router :refer [route]]
    [remlok.loc :refer [pub sub rpub rsub mut mut! syncf ui]]
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
    (when (> (count s) 2)
      (when-not (get-in db [:sugg s])
        `(:sugg ~s)))))
(defmethod rpubf :default []
  nil)

(pub pubf)
(mut mutf)
(rpub rpubf)
(syncf
  (fn [req res]
    #_(println req)
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

#_(defn list []
  (let [search (sub [:search])
        props (reaction
                (let [{:keys [search]} @search]
                  (println 123)
                  ;;@(sub `[(:sugg ~search)])
                  (sub `[(:sugg ~search)])))]
    (fn []
      (let [{:keys [sugg]} @@props]
        [:ul
         (map
           (fn [s]
             [:li (str s)])
           sugg)]))))

(defn list []
  (let [props (reaction
                (let [{:keys [search]} @(sub [:search])]
                  ;;(println 123)
                  @(sub `[(:sugg ~search)])))]
    (fn []
      (let [{:keys [sugg]} @props]
        [:ul
         (map
           (fn [s]
             [:li (str s)])
           sugg)]))))

#_(def input
  (ui
    (constantly [:search])
    (fn [{:keys [search]}]
      [:input
       {:on-change #(mut! `[(:search ~(-> % .-target .-value))])
        :value (str search)}])))

#_(def list
  (ui
    (fn []
      [:search])
    (fn [{:keys [search]}]
      `[(:sugg ~search)])
    (fn [{:keys [sugg] :as p}]
      ;;(println "props " p)
      [:ul
       (map
         (fn [s]
           [:li (str s)])
         sugg)])))

(defn root []
  [:div
   [input]
   [list]])