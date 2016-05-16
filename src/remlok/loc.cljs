(ns remlok.loc
  (:require
    [reagent.core :as r]
    [reagent.ratom :refer [*ratom-context*]]
    [remlok.query :as q]))

;;;;;;;;
;; DB ;;
;;;;;;;;

(def ^:private db
  (r/atom nil))

;;;;;;;;;;
;; Sync ;;
;;;;;;;;;;

(def ^:private sync
  (atom {:scheduled? false
         :reads []
         :muts []
         :send #(%2 nil)
         :merge ()}))

(defn send [f]
  (swap! sync assoc :send f))

(defn nov! [nov]
  (let [{:keys [reads muts]} nov]
    ))

(defn- sync! []
  (let [{:keys [send reads muts]} @sync
        sync* (merge
               (when (seq reads) {:reads reads})
               (when (seq muts) {:muts muts}))]
    (when (seq sync*)
      (send sync* nov!)
      (swap! sync merge {:scheduled? false
                         :reads []
                         :muts []}))))

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

(defn- make-shield [f]
  (binding [*ratom-context* nil]
    (f)))

(def ^:private pubs
  (atom
    {:default (fn [])}))

(def ^:private muts
  (atom
    {:default (fn [db] {:loc db})}))

(defn pub [topic f]
  (swap! pubs assoc topic f))

(defn mut [topic f]
  (swap! muts assoc topic f))

(defn- topic-f [fs topic]
  (get fs topic (get fs :default)))

(defn sub [query]
  (let [topic (q/topic query)
        f (topic-f @pubs topic)
        {:keys [loc rem]} (make-shield #(f db query))]
    (sched-read! rem)
    loc))

(defn mut! [query]
  (let [topic (q/topic query)
        f (topic-f @muts topic)
        {:keys [loc rem]} (make-shield #(f @db query))]
    (sched-mut! rem)
    (reset! db loc)
    nil))