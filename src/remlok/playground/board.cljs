(ns remlok.playground.board
  (:require
    [remlok.loc :as l]
    [remlok.rem :as r]
    [reagent.ratom :refer-macros [reaction]]))

;;;;;;;;;;;;
;; Remote ;;
;;;;;;;;;;;;

(def db
  (atom {}))

(r/pub
  :ads
  (fn []
    @db))

(r/mut
  :ad/new
  (fn [[tmp-id text]]
    ))

;;;;;;;;;;;
;; Local ;;
;;;;;;;;;;;

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
    (let [id (gensym "tmp")
          ad {:id id :text text}]
      {:loc (assoc-in db [:ads id] ad)})))

(defn ads []
  (let [ads (l/sub [:ads])]
    (fn []
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
                     (l/mut! [:ad/new @cur-ad])
                     (l/mut! [:cur-ad ""]))}
        "post!"]])))

(defn root []
  [:div
   [ads]
   [new-ad]])