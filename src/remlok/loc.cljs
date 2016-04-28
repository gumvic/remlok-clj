(ns remlok.loc
  (:refer-clojure :exclude [read])
  (:require
    [reagent.core :as r]
    [reagent.ratom :refer-macros [reaction]]
    [remlok.query :as q]))

(def ^:private db
  (r/atom nil))

#_(def ^:private funs
  (atom
    {:readf (fn [_ _])
     :mutf (fn [db _] db)}))

#_(defn readf! [f]
  (swap! funs assoc :readf f))

#_(defn- read-node [ctx node]
  (let [{:keys [readf]} @funs
        {:keys [attr]} (q/node->ast node)]
    (when-let [val (readf db node ctx)]
      [attr val])))

;; TODO compile query (getting the resolvers at that step)

(def ^:private resfs
  (atom nil))

(def ^:private mutfs
  (atom nil))

(defn pub [attr res]
  (swap! resfs assoc attr res))

(defn mut [attr mut]
  (swap! mutfs assoc attr mut))

(defn- res-node [ctx node]
  (let [{:keys [attr]} (q/node->ast node)
        res (->> (constantly nil)
                 (get @resfs :default)
                 (get @resfs attr))
        val (res ctx node)]
    (when val
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

(defn- mut-node [node]
  (let [{:keys [attr]} (q/node->ast node)
        mut (->> (constantly nil)
                 (get @mutfs :default)
                 (get @mutfs attr))]
    #(mut %)))

(defn- mut-query [query]
  (let [fs (into
             []
             (comp
               (map mut-node)
               (filter some?))
             query)]
    #(reduce
      (fn [db f] (f db))
      %
      fs)))

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