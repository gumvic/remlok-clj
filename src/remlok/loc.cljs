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

(defn mergef
  "Default merge function.
  If args is not nil, will (assoc-in db [topic args] data)
  Otherwise, will (assoc db topic data)"
  [db query data]
  (let [[topic args] query]
    (if (some? args)
      (assoc-in db [topic args] data)
      (assoc db topic data))))

(defn sendf
  "Default send function.
  Doesn't do anything, will emit a warning when used."
  [req _]
  (.warn js/console "This send is omitted (see remlok.loc/send): " (str req)))

(def ^:private sync
  (atom {:scheduled? false
         :reads []
         :muts []
         :send sendf
         :merge {:default mergef}}))

(defn send
  "Sets the send function.
  Send function is (req, res) -> none
  req is {:reads [query0 query1 ...], :muts [query0 query1 ...]}.
  response should be [[query0 data0], [query1 data1], ...].
  The responsibility of the function will be to pass the req to the remote, and call the res with the response."
  [f]
  (swap! sync assoc :send f))

(defn merge
  "Sets the merge function for the topic.
  Merge function is (db, query, data) -> db*
  Note that the db will be already derefed."
  [topic f]
  (swap! sync assoc-in [:merge topic] f))

(defn merge!
  "Merges the novelty.
  nov should be [[query0 data0], [query1 data1], ...]."
  [nov]
  (let [mfs (get @sync :merge)]
    (swap!
      db
      #(reduce
        (fn [db [query data]]
          (let [f (select-fun mfs query)]
            (f db query data)))
        %1
        %2)
      nov)))

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

;; TODO don't include identical queries
(defn- sched-read! [query]
  (swap! sync update :reads conj query)
  (sched-sync!))

(defn- sched-mut! [query]
  (swap! sync update :muts conj query)
  (sched-sync!))

;;;;;;;;;;;;;;;
;; Pub / Sub ;;
;;;;;;;;;;;;;;;

;; TODO should handle vector topics, too
(defn pubf
  "Default publication function.
  If args is not nil, will create a reaction of (get-in @db [topic args])
  Otherwise, will create a reaction of (get @db topic)"
  [db [topic args]]
  {:loc
   (if (some? args)
     (reaction
       (get-in @db [topic args]))
     (reaction
       (get @db topic)))})

;; TODO should handle vector topics, too
(defn mutf
  "Default mutation function.
  Will simply (assoc db topic args)"
  [db [topic args]]
  {:loc (assoc db topic args)})

(def ^:private pubs
  (atom
    {:default pubf}))

(def ^:private muts
  (atom
    {:default mutf}))

(defn pub
  "Publishes the topic using the supplied function.
  The function must be (db, query) -> {:loc reaction, :rem query}
  Both :loc and :rem are optional.
  Note that the db will not be derefed, so that you can build a reaction."
  [topic f]
  (swap! pubs assoc topic f))

(defn mut
  "Sets the mutation handler for the topic using the supplied function.
  The function must be (db, query) -> {:loc db*, :rem query}
  Both :loc and :rem are optional.
  Note that the db will be already derefed."
  [topic f]
  (swap! muts assoc topic f))

(defn read
  "Reads the query using the function set by pub.
  The query is a vector of two, [topic args].
  Both topic and args are arbitrary clojure values.
  Will fallback to the default (pubf) if no function is found for the topic.
  Returns a reaction."
  [query]
  (let [f (select-fun @pubs query)
        {:keys [loc rem]} (make-shield #(f db query))]
    (when (some? rem)
      (sched-read! rem))
    (if (some? loc)
      loc
      (r/atom nil))))

(defn mut!
  "Performs the mutation using the function set by mut.
  The query is a vector of two, [topic args].
  Both topic and args are arbitrary clojure values.
  Will fallback to the default (mutf) if no function is found for the topic.
  Always returns nil."
  [query]
  (let [f (select-fun @muts query)
        {:keys [loc rem]} (make-shield #(f @db query))]
    (when (some? loc)
      (reset! db loc))
    (when (some? rem)
      (sched-mut! rem))
    nil))