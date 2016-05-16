(ns remlok.playground.board
  (:require
    [remlok.loc :as l]
    [remlok.rem :as r]
    [reagent.ratom :refer-macros [reaction]]))

(l/pub
  :ads
  (fn [db]
    {:loc
     (reaction
       (get @db :ads))}))

(l/mut
  :ads/new
  (fn [db [_ text]]
    (let [id (gensym "ad")
          ad {:id id :text text}]
      {:loc (assoc-in db [:ads id] ad)})))

(l/mut
  :cur-text
  (fn [db [_ text]]
    {:loc (assoc db :cur-text text)}))

(defn ads []
  (let [ads (l/sub [:ads])]
    (fn []
      [:ul
       (for [{:keys [id text]} @ads]
         ^{:id id}
         [:li (str text)])])))

(defn input []
  (let [input (l/sub [:input])]
    (fn []
      [:input
       {:on-change #(l/mut! [:input (-> % .-target .-value)])}
       @input])))

(defn root []
  [ads]
  [input])