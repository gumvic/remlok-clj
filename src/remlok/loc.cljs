(ns remlok.loc
  (:require
    [reagent.core :as r]
    [reagent.ratom :refer-macros [reaction]]
    [remlok.query :as q]))

(def ^:private db
  (r/atom nil))

(def ^:private pubs
  (atom nil))

(defn pub [attr fun]
  (swap! pubs assoc attr fun))

(defn- res [attr]
  (->> (constantly nil)
       (get @pubs :default)
       (get @pubs attr)))

(defn- res-node [ctx node]
  (let [{:keys [attr]} (q/node->ast node)
        res (res attr)]
    (when-let [val (res ctx node)]
      [attr val])))

(defn- res-query [ctx query]
  (not-empty
    (into
      {}
      (comp
        (map #(res-node ctx %))
        (filter some?))
      query)))

(defn sub
  ([query]
    (sub {:db db} query))
  ([ctx query]
   (reaction
     (res-query ctx query))))

(defn hub [fun]
  )

(defn disp [msg]
  )

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