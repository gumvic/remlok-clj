(ns remlok.loc
  (:require
    [reagent.core :as r]
    [reagent.ratom
     :refer-macros [reaction]
     :refer [make-reaction IReactiveAtom]]
    [remlok.query :as q]))

;; TODO split into namespaces (should be relatively easy)

(def ^:private db
  (r/atom nil))

;;;;;;;;;;
;; Sync ;;
;;;;;;;;;;

(def ^:private sync
  (atom {:scheduled? false
         :subs []
         :muts []
         :syncf #(%2 nil)
         :mergef merge}))

(defn syncf [f]
  (swap! sync assoc :syncf f))

(defn mergef [f]
  (swap! sync assoc :mergef f))

(defn merge! [tree]
  (let [{:keys [mergef]} @sync]
    (swap! db mergef tree)))

(defn- sync! []
  (let [{:keys [syncf subs muts]} @sync
        sync* (merge
               (when (seq subs) {:subs subs})
               (when (seq muts) {:muts muts}))]
    (when (seq sync*)
      (syncf sync* merge!)
      (swap! sync merge {:scheduled? false
                          :subs []
                          :muts []}))))

(defn- sched-sync! []
  (let [{:keys [scheduled?]} @sync]
    (when-not scheduled?
      (swap! sync assoc :scheduled? true)
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

(def ^:private resfs
  (atom nil))

(def ^:private mutfs
  (atom nil))

(defn pub [attr res]
  (swap! resfs assoc-in [:loc attr] res))

(defn mut [attr mut]
  (swap! mutfs assoc-in [:loc attr] mut))

(defn rpub [attr res]
  (swap! resfs assoc-in [:rem attr] res))

(defn rmut [attr mut]
  (swap! mutfs assoc-in [:rem attr] mut))

(defn- reactive? [x]
  (implements? IReactiveAtom x))

;; TODO refactor sub**, sub***, rsub*, rsub**, mut*, mut**, rmut* - better names

(defn- rsub** [node ctx]
  (let [attr (get (q/node->ast node) :attr)
        f (->> (constantly nil)
               (get-in @resfs [:rem :default])
               (get-in @resfs [:rem attr]))]
    (f @db node ctx)))

(defn- rsub* [query ctx]
  (not-empty
    (into
      []
      (comp
        (map #(rsub** % ctx))
        (filter some?))
      query)))

(defn rsub
  ([query]
    (rsub query nil))
  ([query ctx]
    (rsub* query ctx)))

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
      (not-empty
        (into {} (map sub***) rs)))))

(def ^:private ^:dynamic *in-sub?* false)

(defn sub
  ([query]
    (sub query nil))
  ([query ctx]
    (if *in-sub?*
      (sub* query ctx)
      (binding [*in-sub?* true]
        (sched-sub!
          (rsub query))
        (sub* query ctx)))))

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

(defn mut! [query]
  (sched-mut!
    (rmut query))
  (swap! db mut* query)
  nil)