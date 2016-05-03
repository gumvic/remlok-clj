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

#_(def ^:private resfs
  (atom nil))

#_(def ^:private mutfs
  (atom nil))

(def ^:private pubf
  (atom (fn [])))

(def ^:private mutf
  (atom (fn [])))

(def ^:private rpubf
  (atom (fn [])))

(def ^:private rmutf
  (atom (fn [])))

(defn pub [f]
  (reset! pubf f))

(defn mut [f]
  (reset! mutf f))

(defn rpub [f]
  (reset! rpubf f))

(defn rmut [f]
  (reset! rmutf f))

#_(defn rmut [attr mut]
  (swap! mutfs assoc-in [:rem attr] mut))

#_(defn- reactive? [x]
  (implements? IReactiveAtom x))

;; TODO refactor sub**, sub***, rsub*, rsub**, mut*, mut**, rmut* - better names

#_(defn- rsub** [node ctx]
  (let [attr (get (q/node->ast node) :attr)
        f (->> (constantly nil)
               (get-in @resfs [:rem :default])
               (get-in @resfs [:rem attr]))]
    (f @db node ctx)))

(defn- rsub** [node ctx]
  (@rpubf @db node ctx))

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

#_(defn- sub*** [[attr res]]
  (if (reactive? res)
    [attr @res]
    [attr res]))

#_(defn- sub** [node ctx]
  (let [attr (get (q/node->ast node) :attr)
        f (->> (constantly nil)
               (get-in @resfs [:loc :default])
               (get-in @resfs [:loc attr]))
        res (f db node ctx)
        res (if (fn? res)
              (make-reaction res)
              res)]
    [attr res]))

#_(defn- sub* [query ctx]
  (let [rs (mapv #(sub** % ctx) query)]
    (reaction
      (not-empty
        (into {} (map sub***) rs)))))

(defn- sub*** [[attr r]]
  [attr @r])

(defn- sub** [node ctx]
  (let [attr (-> node q/node->ast :attr)
        r (@pubf db node ctx)]
    [attr r]))

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

#_(defn- mut** [db node]
  (let [attr (get (q/node->ast node) :attr)
        f (->> (fn [db _] db)
               (get-in @mutfs [:loc :default])
               (get-in @mutfs [:loc attr]))]
    (f db node)))

(defn- mut* [db query]
  (reduce @mutf db query))

#_(defn- rmut* [node]
  (let [attr (get (q/node->ast node) :attr)
        f (->> (fn [_ _] nil)
               (get-in @mutfs [:rem :default])
               (get-in @mutfs [:rem attr]))]
    (f db node)))

(defn- rmut* [node]
  (@rmutf @db node))

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