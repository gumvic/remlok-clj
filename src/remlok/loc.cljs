(ns remlok.loc
  (:require
    [reagent.core :as re]
    [reagent.ratom :refer [make-reaction]]
    [remlok.query :as q]))

(def ^:private db
  (re/atom nil))

(def ^:private pubs
  (atom nil))

(defn pub [attr fun]
  (swap! pubs assoc attr fun))

(defn- attr-pub [attr]
  (->> (constantly nil)
       (get @pubs :default)
       (get @pubs attr)))

(defn- query-node [ctx node]
  (let [{:keys [attr]} (q/node->ast node)
        pub (attr-pub attr)]
    (when-let [val (pub ctx node)]
      [attr val])))

(defn- query-query [ctx query]
  (not-empty
    (into
      {}
      (comp
        (map #(query-node ctx %))
        (filter some?))
      query)))

(defn sub
  ([query]
    (sub {:db db} query))
  ([ctx query]
   (make-reaction
     #(query-query ctx query))))

(defn mount! [ui el]
  (re/render ui el))

;;;;;;;;;;
;; Sync ;;
;;;;;;;;;;

#_(defn merge! [app query tree]
  (let [{:keys [db normf mergef]} app
        attrs (query->attrs query)
        db* (normf tree)]
    (vswap! db mergef db*)
    (doseq [id (ui-by-attrs app attrs)]
      (ui-sync-loc! app id))))

#_(defn merge! [app query tree])

#_(defn- sync! [app]
  (let [{:keys [state syncf]} app
        {{:keys [reads muts]} :sync} @state
        sync (merge
               (when (seq reads) {:reads reads})
               (when (seq muts) {:muts muts}))]
    (when (seq sync)
      (syncf
        sync
        #(doseq [[query tree] %]
          (merge! app query tree))))
    (vswap! state assoc :sync {:scheduled? false
                               :reads []
                               :muts []})))

;; TODO use goog nextTick
#_(defn- schedule-sync! [app]
  (let [{:keys [state]} app
        {{:keys [scheduled?]} :sync} @state]
    (when-not scheduled?
      (vswap! state assoc-in [:sync :scheduled?] true)
      (js/setTimeout
        #(sync! app)
        0))))

#_(defn- schedule-read! [app query]
  (when (seq query)
    (let [{:keys [state]} app]
      (vswap! state update-in [:sync :reads] conj query)
      (schedule-sync! app))))

#_(defn- schedule-mut! [app query]
  (when (seq query)
    (let [{:keys [state]} app]
      (vswap! state update-in [:sync :muts] conj query)
      (schedule-sync! app))))