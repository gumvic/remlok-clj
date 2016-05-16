(ns remlok.playground.wiki
  (:import [goog Uri]
           [goog.net Jsonp])
  (:require
    [reagent.ratom :refer-macros [reaction]]
    [remlok.loc :as l]
    [remlok.rem :as r]
    [remlok.query :as q]
    [cljs.core.async :as a :refer [chan put! take!]]))

;;;;;;;;;;;;
;; Remote ;;
;;;;;;;;;;;;

(defn wiki [s]
  (let [uri "http://en.wikipedia.org/w/api.php?action=opensearch&format=json&search="
        gjsonp (Jsonp. (Uri. (str uri s)))
        ch (chan)]
    (.send gjsonp nil #(put! ch (second %)))
    ch))

(r/pub
  :sugg
  (fn [query]
    (let [s (q/args query)]
      (wiki s))))

(defn receive [{:keys [reads]} res]
  (let [reads* (a/map
                 vector
                 (for [q reads
                       :let [ch (r/read q)]]
                   (let [ch* (chan)]
                     (take! ch #(put! ch* [q %]))
                     ch*)))]
    (take! reads* #(res {:reads %}))))

;;;;;;;;;;;
;; Local ;;
;;;;;;;;;;;

(l/pub
  :search
  (fn [db]
    {:loc (reaction
            (get @db :search))}))

(l/pub
  :sugg
  (fn [db query]
    (let [s (q/args query)]
      {:loc
       (reaction
         (get-in @db [:sugg s]))
       :rem
       (when
         (and
           (> (count s) 2)
           (not (get-in @db [:sugg s])))
         [:sugg s])})))

(l/mut
  :search
  (fn [db query]
    (let [s (q/args query)]
      {:loc (assoc db :search s)})))

(l/merge
  :sugg
  (fn [db query data]
    (let [s (q/args query)]
      (assoc-in db [:sugg s] data))))

(l/send
  (fn [req res]
    (receive req res)))

(defn input []
  (let [search (l/sub [:search])]
    (fn []
      [:input
       {:on-change #(l/mut! [:search (-> % .-target .-value)])
        :value (str @search)}])))

(defn list []
  (let [search (l/sub [:search])
        sugg (reaction
               @(l/sub [:sugg @search]))]
    (fn []
      [:ul
       (for [s @sugg]
         ^{:key (str s)}
         [:li (str s)])])))

(defn root []
  [:div
   [input]
   [list]])