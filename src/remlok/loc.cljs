(ns remlok.loc
  (:refer-clojure :exclude [read])
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

#_(defn- res-node [node ctx]
  (let [{:keys [attr]} (q/node->ast node)
        res (->> (constantly nil)
                 (get @resfs :default)
                 (get @resfs attr))
        val (res db node ctx)]
    (when val
      [attr val])))

#_(defn- res* [query ctx]
  (not-empty
    (into
      {}
      (comp
        (map #(res-node % ctx))
        (filter some?))
      query)))

(defn- sub* [query ctx]
  (let [xs (mapv
             #(let [attr (get (q/node->ast %) :attr)
                    f (->> (constantly nil)
                           (get @resfs :default)
                           (get @resfs attr))]
               [attr (f db query ctx)])
             query)
        {:keys [consts reacts]} (group-by
                                  #(if (fn? (second %)) :reacts :consts)
                                  xs)
        reacts (mapv #(vector (first %) (make-reaction (second %))) reacts)]
    (reaction
      (merge
        consts
        (map #(vector (first %) @(second %)) reacts)))))

(defn sub
  ([query]
    (sub query nil))
  ([query ctx]
    (sub* query ctx)))

(defn- mut-node [node]
  (let [{:keys [attr]} (q/node->ast node)
        mut (->> (constantly nil)
                 (get @mutfs :default)
                 (get @mutfs attr))]
    #(mut % node)))

(defn- mut-query [query]
  #(reduce
    (fn [db f] (f db))
    %
    (into
      []
      (comp
        (map mut-node)
        (filter some?))
      query)))

(defn mut! [query]
  (swap! db (mut-query query))
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