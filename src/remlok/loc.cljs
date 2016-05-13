(ns remlok.loc
  (:require
    [reagent.core :as r]
    [reagent.ratom
     :refer-macros [reaction]
     :refer [*ratom-context* make-reaction IReactiveAtom]]
    [remlok.query :as q]))

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

(defn- sched-mut! [node]
  (when node
    (swap! sync update :muts conj node)
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
  (atom
    {:default (fn [] nil)}))

(def ^:private muts
  (atom
    {:default (fn [db] {:loc db})}))

(defn pub [attr f]
  (swap! pubs assoc attr f))

(defn mut [attr f]
  (swap! muts assoc attr f))

#_(defn- rsub** [node ctx]
  (let [attr (q/attr node)
        f (->>
            (constantly nil)
            (get-in @pubs [:rem :default])
            (get-in @pubs [:rem attr]))]
    (f (peek db) node ctx)))

#_(defn- rsub* [query ctx]
  (not-empty
    (into
      []
      (comp
        (map #(rsub** % ctx))
        (filter some?))
      query)))

#_(defn rsub
  ([query]
    (rsub query nil))
  ([query ctx]
    (rsub* query ctx)))

#_(defn- sub** [node ctx]
  (let [attr (q/attr node)
        f (->>
            (constantly nil)
            (get-in @pubs [:loc :default])
            (get-in @pubs [:loc attr]))
        r (f db node ctx)]
    (when r
      [attr r])))

#_(defn- sub* [query ctx]
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

#_(def ^:private ^:dynamic *in-sub?* false)

#_(defn sub
  ([query]
   (sub query nil))
  ([query ctx]
   (if *in-sub?*
     (sub* query ctx)
     (binding [*in-sub?* true]
       (sched-sub!
         (rsub query))
       (sub* query ctx)))))

;; TODO recursive subs
;; TODO shield everything under sub
(defn- sub* [node]
  (let [attr (q/attr node)
        df (get @pubs :default)
        f (get @pubs attr df)
        {:keys [loc rem]} (f db node)
        loc (when loc [attr loc])]
    {:loc loc
     :rem rem}))

(defn sub [query]
  (let [rs (mapv sub* query)
        loc (into [] (comp (map :loc) (filter some?)) rs)
        rem (into [] (comp (map :rem) (filter some?)) rs)]
    (sched-sub! rem)
    (reaction
      (into
        {}
        (for [[a r] loc]
          [a @r])))))

(defn mut! [node]
  (let [attr (q/attr node)
        df (get @muts :default)
        f (get @muts attr df)
        {:keys [loc rem]} (f (peek db) node)]
    (sched-mut! rem)
    (reset! db loc)
    nil))