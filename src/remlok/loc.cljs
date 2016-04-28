(ns remlok.loc
  (:require
    [reagent.core :as r]
    [reagent.ratom :refer-macros [reaction] :refer [make-reaction]]
    [remlok.query :as q]))

(def ^:private db
  (r/atom nil))

(def ^:private resfs
  (atom nil))

(def ^:private mutfs
  (atom nil))

(defn pub [attr res]
  (swap! resfs assoc attr res))

(defn mut [attr mut]
  (swap! mutfs assoc attr mut))

(defn- reactive? [x]
  (satisfies? IDeref x))

;; TODO refactor sub**, sub*** - better names
(defn- sub** [node ctx]
  (let [attr (get (q/node->ast node) :attr)
        f (->> (constantly nil)
               (get @resfs :default)
               (get @resfs attr))
        res (f db node ctx)
        res (if (fn? res)
              (make-reaction res)
              res)]
    [attr res]))

(defn- sub*** [[attr res]]
  (if (reactive? res)
    [attr @res]
    [attr res]))

(defn- sub* [query ctx]
  (let [rs (mapv #(sub** % ctx) query)]
    (reaction
      (into {} (map sub***) rs))))

(defn sub
  ([query]
    (sub query nil))
  ([query ctx]
    (sub* query ctx)))

;; TODO refactor mut** - better name
(defn- mut** [db node]
  (let [attr (get (q/node->ast node) :attr)
        f (->> (fn [db _] db)
               (get @mutfs :default)
               (get @mutfs attr))]
    (f db node)))

(defn- mut* [db query]
  (reduce mut** db query))

(defn mut! [query]
  (swap! db mut* query)
  nil)

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