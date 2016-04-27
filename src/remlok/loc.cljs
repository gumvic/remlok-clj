(ns remlok.loc
  (:refer-clojure :exclude [read])
  (:require
    [reagent.core :as r]
    [reagent.ratom :refer-macros [reaction]]
    [remlok.query :as q]))

(def ^:private db
  (r/atom nil))

(def ^:private funs
  (atom
    {:readf (fn [_ _])
     :mutf (fn [db _] db)}))

(defn readf! [f]
  (swap! funs assoc :readf f))

(defn- read-node [ctx node]
  (let [{:keys [readf]} @funs
        {:keys [attr]} (q/node->ast node)]
    (when-let [val (readf db node ctx)]
      [attr val])))

(defn- read-query [ctx query]
  (not-empty
    (into
      {}
      (comp
        (map #(read-node ctx %))
        (filter some?))
      query)))

(defn read
  ([query]
    (read {:db db} query))
  ([ctx query]
   (reaction
     (read-query ctx query))))

(defn mutf! [f]
  (swap! funs assoc :mutf f))

(defn mut! [query]
  #_(let [{:keys [transf]} @funs]
    (swap! db transf msg)))

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