(ns remlok.playground.wiki
  (:import [goog Uri]
           [goog.net Jsonp])
  (:require
    [reagent.ratom :refer-macros [reaction]]
    [remlok.loc :as l]
    [remlok.rem :as r]
    [cljs.core.async :as a :refer [chan put! take!]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Wiki Auto Completion ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Inspired by the om.next example of the same functionality.
;; Features:
;; 1) Remote reads.
;; 2) Asynchronous processing on the remote using core.async.
;; 3) Caching on the local, so that unnecessary remote reads are not made.

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
  (fn [_ [_ search]]
    (wiki search)))

(defn receive [{:keys [reads]} res]
  (let [reads* (a/map
                 vector
                 (for [q reads
                       :let [ch (r/read nil q)]]
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
  (fn [db [_ search]]
    {:loc
     (reaction
       (get-in @db [:sugg search]))
     :rem
     (when
       (and
         (> (count search) 2)
         (not (get-in @db [:sugg search])))
       [:sugg search])}))

(l/mut
  :search
  (fn [db [_ search]]
    {:loc (assoc db :search search)}))

(l/merge
  :sugg
  (fn [db [_ search] data]
    (assoc-in db [:sugg search] data)))

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