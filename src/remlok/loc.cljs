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

;; TODO default nov-read doesn't know what to do with joins
(def ^:private syncing
  (atom {:scheduled? false
         :reads []
         :muts []
         :sync #(%2 nil)
         :nov-read (fn [db query tree]
                     (reduce
                       #(let [{:keys [attr args]} (q/ast %2)
                              tree* (get tree attr)]
                         (if args
                           (assoc-in %1 [attr args] tree*)
                           (assoc %1 attr tree*)))
                       db
                       query))
         :nov-mut (fn [db] db)}))

(defn sync [f]
  (swap! syncing assoc :sync f))

(defn- nov* [db f nov]
  (reduce
    (fn [db [q n]]
      (f db q n))
    db
    nov))

(defn nov! [nov]
  (let [{:keys [nov-read nov-mut]} @syncing
        {:keys [reads muts]} nov]
    (swap!
      db
      #(-> %
           (nov* nov-read reads)
           (nov* nov-mut muts)))))

(defn- sync! []
  (let [{:keys [sync reads muts]} @syncing
        sync* (merge
               (when (seq reads) {:reads reads})
               (when (seq muts) {:muts muts}))]
    (when (seq sync*)
      (sync sync* nov!)
      (swap! syncing merge {:scheduled? false
                            :reads []
                            :muts []}))))

(defn- sched-sync! []
  (let [{:keys [scheduled?]} @syncing]
    (when-not scheduled?
      (swap! syncing assoc :scheduled? true)
      (js/setTimeout sync! 0))))

(defn- sched-read! [query]
  (when (seq query)
    (swap! syncing update :reads conj query)
    (sched-sync!)))

(defn- sched-mut! [node]
  (when node
    (swap! syncing update :muts conj node)
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
    {:default (fn [])}))

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
(defn- sub* [node]
  (let [attr (q/attr node)
        df (get @pubs :default)
        f (get @pubs attr df)
        {:keys [loc rem]} (f db node)
        loc (when loc [attr loc])]
    {:loc loc
     :rem rem}))

(defn sub [query]
  (let [rs (binding [*ratom-context* nil]
             (mapv sub* query))
        loc (into [] (comp (map :loc) (filter some?)) rs)
        rem (into [] (comp (map :rem) (filter some?)) rs)]
    (sched-read! rem)
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