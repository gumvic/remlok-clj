(ns remlok.loc
  (:require
    [reagent.core :as r]
    [reagent.ratom
     :refer-macros [reaction]
     :refer [make-reaction]]
    [remlok.query :as q]))

;; TODO split into namespaces (should be relatively easy)

(def ^:private db
  (r/atom nil))

(def ^:private resfs
  (atom nil))

(def ^:private mutfs
  (atom nil))

(def ^:private sync
  (atom {:scheduled? false
         :subs []
         :muts []
         :syncf #(%2 nil)
         :mergef merge}))

;;;;;;;;;;
;; Sync ;;
;;;;;;;;;;

(defn merge! [tree]
  (let [{:keys [mergef]} @sync]
    (swap! db mergef tree)))

(defn- sync! []
  (let [{:keys [syncf subs muts]} @sync
        sync (merge
               (when (seq subs) {:subs subs})
               (when (seq muts) {:muts muts}))]
    (when (seq sync)
      (syncf sync merge!)
      (vswap! sync merge {:scheduled? false
                          :subs []
                          :muts []}))))

(defn- sched-sync! []
  (let [{:keys [scheduled?]} @sync]
    (when-not scheduled?
      (vswap! sync assoc :scheduled? true)
      (js/setTimeout sync! 0))))

(defn- sched-sub! [query]
  (when (seq query)
    (swap! sync update :subs conj query)
    (sched-sync!)))

(defn- sched-mut! [query]
  (when (seq query)
    (swap! sync update :muts conj query)
    (sched-sync!)))

;;;;;;;;;;;;;;;
;; Pub / Sub ;;
;;;;;;;;;;;;;;;

(defn pub [attr res]
  (swap! resfs assoc-in [:loc attr] res))

(defn mut [attr mut]
  (swap! mutfs assoc-in [:loc attr] mut))

(defn rpub [attr res]
  (swap! resfs assoc-in [:rem attr] res))

(defn rmut [attr mut]
  (swap! mutfs assoc-in [:rem attr] mut))

(defn- reactive? [x]
  (satisfies? IDeref x))

(def ^:private ^:dynamic *in-sub?* false)
(def ^:private ^:dynamic *rsub*)

;; TODO refactor sub**, sub*** - better names

(defn- sub*** [[attr res]]
  (if (reactive? res)
    [attr @res]
    [attr res]))

(defn- sub** [node ctx]
  (let [attr (get (q/node->ast node) :attr)
        f (->> (constantly nil)
               (get-in @resfs [:loc :default])
               (get-in @resfs [:loc attr]))
        res (f db node ctx)
        res (if (fn? res)
              (make-reaction res)
              res)]
    [attr res]))

(defn- sub* [query ctx]
  (let [rs (mapv #(sub** % ctx) query)]
    (reaction
      (into {} (map sub***) rs))))

(defn sub
  ([query]
    (sub query nil))
  ([query ctx]
    (if *in-sub?*
      (sub* query ctx)
      (binding [*in-sub?* true
                *rsub* (volatile! [])]
        (let [sub (sub* query ctx)]
          (sched-sub! @*rsub*)
          sub)))))

;; TODO refactor mut** - better name
(defn- mut** [db node]
  (let [attr (get (q/node->ast node) :attr)
        f (->> (fn [db _] db)
               (get-in @mutfs [:loc :default])
               (get-in @mutfs [:loc attr]))]
    (f db node)))

(defn- mut* [db query]
  (reduce mut** db query))

(defn- rmut* [node]
  (let [attr (get (q/node->ast node) :attr)
        f (->> (fn [_ _] nil)
               (get-in @mutfs [:rem :default])
               (get-in @mutfs [:rem attr]))]
    (f db node)))

(defn- rmut [query]
  (into
    []
    (comp
      (map rmut*)
      (filter some?))
    query))

;; TODO allow recursive mutations?
(defn mut! [query]
  (sched-mut!
    (rmut query))
  (swap! db mut* query)
  nil)