(ns remlok.loc
  (:refer-clojure :exclude [read merge])
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

(defn mergef [db [topic args] data]
  (if (some? args)
    (assoc-in db [topic args] data)
    (assoc db topic data)))

(defn sendf [req]
  (.warn js/console "This send is omitted (see remlok.loc/send): " (str req)))

;; TODO consider adding serialize/deserialize
(def ^:private sync
  (atom {:scheduled? false
         :reads []
         :muts []
         :send sendf
         :merge {:default mergef}}))

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
  (swap! sync update :reads conj query)
  (sched-sync!))

(defn- sched-mut! [query]
  (swap! sync update :muts conj query)
  (sched-sync!))

;;;;;;;;;;;;;;;
;; Pub / Sub ;;
;;;;;;;;;;;;;;;

;; TODO should this try to synchronize if there's nothing in the db?
(defn pubf [db [topic args]]
  {:loc
   (if (some? args)
     (reaction
       (get-in @db [topic args]))
     (reaction
       (get @db topic)))})

;; TODO should this try to synchronize and how?
(defn mutf [db [topic args]]
  {:loc (assoc db topic args)})

(def ^:private pubs
  (atom
    {:default pubf}))

(def ^:private muts
  (atom
    {:default mutf}))

(defn pub [topic f]
  (swap! pubs assoc topic f))

(defn mut [topic f]
  (swap! muts assoc topic f))

(defn read [query]
  (let [f (select-fun @pubs query)
        locrem (make-shield #(f db query))
        loc (get locrem :loc ::none)
        rem (get locrem :rem ::none)]
    (when (and
            (not= rem ::none)
            (some? rem))
      (sched-read! rem))
    (if (and
          (not= loc ::none)
          (some? loc))
      loc
      (r/atom nil))))

(defn mut! [query]
  (let [f (select-fun @muts query)
        locrem (make-shield #(f @db query))
        loc (get locrem :loc ::none)
        rem (get locrem :rem ::none)]
    (when (not= loc ::none)
      (reset! db loc))
    (when (and
            (not= rem ::none)
            (some? rem))
      (sched-mut! rem))
    nil))