(ns remlok.loc
  (:require
    [reagent.core :as r]
    [reagent.ratom
     :refer-macros [reaction]
     :refer [*ratom-context* make-reaction IReactiveAtom]]
    [remlok.query :as q]))

;; TODO split into namespaces (should be relatively easy)

;;;;;;;;
;; DB ;;
;;;;;;;;

(def ^:private db
  (r/atom nil))

;;;;;;;;;;
;; Sync ;;
;;;;;;;;;;

(defn deep-merge [a b]
  (if (and (map? a) (map? b))
    (merge-with deep-merge a b)
    b))

(def ^:private sync
  (atom {:scheduled? false
         :subs []
         :muts []
         :syncf #(%2 nil)
         :mergef deep-merge}))

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

(defn- reactive? [x]
  (implements? IReactiveAtom x))

(defn- make-shield [f]
  (binding [*ratom-context* nil]
    (f)))

(defn- peek [r]
  (binding [*ratom-context* nil]
    (-deref r)))

(def ^:private pubs
  (atom {}))

(def ^:private muts
  (atom {}))

(defn pub [attr f]
  (swap! pubs assoc-in [:loc attr] f))

(defn mut [attr f]
  (swap! muts assoc-in [:loc attr] f))

(defn rpub [attr f]
  (swap! pubs assoc-in [:rem attr] f))

(defn rmut [attr f]
  (swap! muts assoc-in [:rem attr] f))

(defn- rsub** [node ctx]
  (let [attr (q/attr node)
        f (->>
            (constantly nil)
            (get-in @pubs [:rem :default])
            (get-in @pubs [:rem attr]))]
    (f (peek db) node ctx)))

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

(defn- sub** [node ctx]
  (let [attr (q/attr node)
        f (->>
            (constantly nil)
            (get-in @pubs [:loc :default])
            (get-in @pubs [:loc attr]))
        r (f db node ctx)]
    (when r
      [attr r])))

(defn- sub* [query ctx]
  (let [rs (into
             []
             (comp
               (map #(sub** % ctx))
               (filter some?))
             query)]
    (reaction
      (not-empty
        (into
          {}
          (for [[a r] rs]
            [a @r]))))))

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
  (let [attr (q/attr node)
        f (->>
            (fn [db] db)
            (get-in @muts [:loc :default])
            (get-in @muts [:loc attr]))]
    (f db node)))

(defn- mut* [db query]
  (reduce mut** db query))

(defn- rmut* [node]
  (let [attr (q/attr node)
        f (->>
            (constantly nil)
            (get-in @muts [:rem :default])
            (get-in @muts [:rem attr]))]
    (f (peek db) node)))

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