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

(defn- sub* [query ctx]
  (let [f @pubf
        rs (into
             []
             (for [node query
                   :let [a (-> node q/node->ast :attr)
                         r (f db node ctx)]
                   :when (some? r)]
               [a r]))]
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
    ;;(println "sub " query)
   (if *in-sub?*
     (sub* query ctx)
     (binding [*in-sub?* true]
       (sched-sub!
         (rsub query))
       (sub* query ctx)))))

(defn- mut* [db query]
  (reduce @mutf db query))

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

(defn ui
  ([render]
    render)
  ([query render]
    (fn [& args]
      (let [props (sub (apply query args))]
        (fn [& args]
          (apply render @props args))))))

(defn ui [& steps]
  (if-let [rend (last steps)]
    (if-let [[q & qs] (seq (butlast steps))]
      (fn [& args]
        (let [props (reduce
                      (fn [r q]
                        (sub (apply q @(reaction @r) args))
                        #_(reaction
                          (println 123)
                          @(sub (apply q @r args))))
                      (sub (apply q args))
                      qs)]
          (fn [& args]
            (apply rend @props args))))
      rend)
    (fn [])))

(defn ui
  ([rend]
    rend)
  ([props rend]
    (fn [& args]
      (let [props* (sub (apply props args))]
        (fn [& args]
          (apply rend @props* args)))))
  ([ops props rend]
    (fn [& args]
      (let [ops* (sub (apply ops args))
            ;;ops* (reaction nil)
            props* (reaction
                     ;;(println 123)
                     (sub (apply props @ops* args)))
            props** (reaction @@props*)]
        (fn [& args]
          (apply rend @props** args))))))