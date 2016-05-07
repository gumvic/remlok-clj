(ns remlok.impl.loc.pub
  (:require
    [reagent.ratom
     :refer-macros [reaction]
     :refer [*ratom-context* make-reaction IReactiveAtom]]
    [remlok.impl.loc.db :refer [db]]
    [remlok.impl.loc.sync :refer [sched-sub!]]
    [remlok.query :as q]))

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

(defn pub [attr f]
  (swap! pubs assoc-in [:loc attr] f))

(defn rpub [attr f]
  (swap! pubs assoc-in [:rem attr] f))

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