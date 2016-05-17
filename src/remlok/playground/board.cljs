(ns remlok.playground.board
  (:require
    [remlok.loc :as l]
    [remlok.rem :as r]
    [reagent.ratom :refer-macros [reaction]]
    [clojure.string :refer [capitalize]]))

;;;;;;;;;;;
;; Board ;;
;;;;;;;;;;;
;; This features:
;; 1) Remote reads and mutations (artificial delay of 1000 is set to emulate network).
;; 2) Optimistic mutations.
;; 3) Both client and server mutation verification for different cases.
;; 4) Patching of temporary ids.
;; You can add ads to the board, so that:
;; 1) Ad can not be empty (this is verified by the local).
;; 2) Ad can not be a duplicate (this is verified by the remote).
;; 3) Ad should be properly capitalized (this is verified by the remote).

;;;;;;;;;;;;
;; Remote ;;
;;;;;;;;;;;;

(let [id-a (random-uuid)
      id-b (random-uuid)
      ad-a {:id id-a
            :text "Clojure job"
            :created (.getTime (js/Date.))}
      ad-b {:id id-b
            :text "Java job"
            :created (inc (.getTime (js/Date.)))}]
  (def db
    (atom
      {id-a ad-a
       id-b ad-b})))

(r/pub
  :ads
  (fn [db _]
    @db))

(r/mut
  :ad/new
  (fn [db [_ {:keys [text] :as ad}]]
    (let [text (capitalize text)
          existing (first
                     (filter
                       (fn [[_ {text* :text}]]
                         (= text* text))
                       @db))]
      (when-not existing
        (let [id (random-uuid)
              ad* (assoc ad
                    :id id
                    :text text)]
          (swap! db assoc id ad*)
          ad*)))))

(defn receive [{:keys [reads muts]} res]
  (let [reads* (for [q reads]
                 [q (r/read db q)])
        muts* (for [q muts]
                [q (r/mut! db q)])
        res* {:reads reads*
              :muts muts*}]
    (res res*)))

;;;;;;;;;;;
;; Local ;;
;;;;;;;;;;;

(l/pub
  :ads
  (fn [db]
    {:loc
          (reaction
            (sort-by
              :created
              (vals (get @db :ads))))
     :rem [:ads]}))

(l/mut
  :ad/new
  (fn [db [_ text]]
    (if-not (empty? text)
      (let [id (gensym "tmp")
            ad {:id id
                :text text
                :created (.getTime (js/Date.))}]
        {:loc (update db :ads assoc id ad)
         :rem [:ad/new ad]})
      {:loc db})))

(l/send
  (fn [req res]
    (js/setTimeout
      #(receive req res)
      1000)))

(l/merge
  :ad/new
  (fn [db [_ {tmp-id :id}] {:keys [id] :as ad}]
    (let [db* (update db :ads dissoc tmp-id)]
      (if ad
        (update db* :ads assoc id ad)
        db*))))

(defn ads []
  (let [ads (l/sub [:ads])]
    (fn []
      (if (seq @ads)
        [:ul
         (for [{:keys [id text]} @ads]
           ^{:key (str id)}
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