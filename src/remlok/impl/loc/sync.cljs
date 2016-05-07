(ns remlok.impl.loc.sync
  (:require
    [remlok.impl.loc.db :refer [db]]))

(defn- deep-merge [a b]
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

(defn sched-sub! [query]
  (when (seq query)
    (swap! sync update :subs conj query)
    (sched-sync!)))

(defn sched-mut! [query]
  (when (seq query)
    (swap! sync update :muts conj query)
    (sched-sync!)))