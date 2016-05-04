(ns remlok.playground.wiki
  (:import [goog Uri]
           [goog.net Jsonp])
  (:require
    [reagent.ratom :refer-macros [reaction]]
    [remlok.router :refer [route]]
    [remlok.loc :refer [pub sub rpub rsub mut mut! syncf mergef]]
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

(defn deep-merge [a b]
  (if (and (map? a) (map? b))
    (merge-with deep-merge a b)
    b))

(pub pubf)
(mut mutf)
(rpub rpubf)
(mergef deep-merge)
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