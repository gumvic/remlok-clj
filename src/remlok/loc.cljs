(ns remlok.loc
  (:refer-clojure :exclude [merge])
  (:require
    [reagent.core :as r]
    [reagent.ratom
     :refer-macros [reaction]
     :refer [*ratom-context*]]))

;;;;;;;;;;;;;
;; Helpers ;;
;;;;;;;;;;;;;

(defn- select-fun [fs [topic _]]
  (get fs topic (get fs :default)))

(defn- make-shield [f]
  (binding [*ratom-context* nil]
    (f)))

;;;;;;;;
;; DB ;;
;;;;;;;;

(def ^:private db
  (r/atom nil))

;;;;;;;;;;
;; Sync ;;
;;;;;;;;;;

;; TODO add serialize/deserialize
(def ^:private sync
  (atom {:scheduled? false
         :reads []
         :muts []
         :send #(%2 nil)
         :merge {:default
                 (fn [db [topic args] data]
                   (if (some? args)
                     (assoc-in db [topic args] data)
                     (assoc db topic data)))}}))

(defn send [f]
  (swap! sync assoc :send f))

(defn merge [topic f]
  (swap! sync assoc-in [:merge topic] f))

(defn merge! [nov]
  (let [{:keys [reads muts]} nov
        mfs (get @sync :merge)]
    (swap!
      db
      #(reduce
        (fn [db [query data]]
          (let [f (select-fun mfs query)]
            (f db query data)))
        %1
        %2)
      (concat reads muts))))

(defn- sync! []
  (let [{:keys [send reads muts]} @sync
        req (cljs.core/merge
              (when (seq reads) {:reads reads})
              (when (seq muts) {:muts muts}))]
    (when (seq req)
      (send req merge!)
      (swap!
        sync
        assoc
        :scheduled? false
        :reads []
        :muts []))))

(defn- sched-sync! []
  (let [{:keys [scheduled?]} @sync]
    (when-not scheduled?
      (swap! sync assoc :scheduled? true)
      (js/setTimeout sync! 0))))

(defn- sched-read! [query]
  (when query
    (swap! sync update :reads conj query)
    (sched-sync!)))

(defn- sched-mut! [query]
  (when query
    (swap! sync update :muts conj query)
    (sched-sync!)))

;;;;;;;;;;;;;;;
;; Pub / Sub ;;
;;;;;;;;;;;;;;;

;; TODO should default pub try to synchronize if there's nothing in the db?
(def ^:private pubs
  (atom
    {:default (fn [db [topic args]]
                {:loc (if (some? args)
                        (reaction
                          (get-in @db [topic args]))
                        (reaction
                          (get @db topic)))})}))

;; TODO should default mut try to synchronize?
(def ^:private muts
  (atom
    {:default (fn [db [topic args]]
                {:loc (assoc db topic args)})}))

(defn pub [topic f]
  (swap! pubs assoc topic f))

(defn mut [topic f]
  (swap! muts assoc topic f))

(defn sub [query]
  (let [f (select-fun @pubs query)
        {:keys [loc rem]} (make-shield #(f db query))]
    (sched-read! rem)
    loc))

(defn mut! [query]
  (let [f (select-fun @muts query)
        {:keys [loc rem]} (make-shield #(f @db query))]
    (sched-mut! rem)
    (reset! db loc)
    nil))