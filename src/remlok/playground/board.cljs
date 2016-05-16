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
       (vals
         (get @db :ads)))}))

(l/mut
  :ad/new
  (fn [db [_ text]]
    (let [id (gensym "ad")
          ad {:id id :text text}]
      {:loc (assoc-in db [:ads id] ad)})))

(defn ads []
  (println 123)
  (let [ads (l/sub [:ads])]
    (fn []
      (println @ads)
      (if (seq @ads)
        [:ul
         (for [{:keys [id text]} @ads]
           ^{:id id}
           [:li "[" (str id) "]" " " (str text)])]
        [:span "No Ads Yet"]))))

(defn new-ad []
  (let [cur-ad (l/sub [:cur-ad])]
    (fn []
      [:div
       [:input
        {:on-change #(l/mut! [:cur-ad (-> % .-target .-value)])
         :value @cur-ad}]
       [:button
        {:on-click #(do
                     (l/mut! [:cur-ad ""])
                     (l/mut! [:ad/new @cur-ad]))}
        "post!"]])))

(defn root []
  [ads]
  [new-ad])