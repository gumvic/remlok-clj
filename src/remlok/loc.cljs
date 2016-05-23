(ns remlok.loc
  (:refer-clojure :exclude [read merge])
  (:require
    [remlok.impl :refer [handlers handle handler]]
    [reagent.core :as r]
    [reagent.ratom
     :refer-macros [reaction]
     :refer [*ratom-context*]]))

;;;;;;;;;;;;;
;; Helpers ;;
;;;;;;;;;;;;;

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
  If topic is a vector, will (assoc-in db topic data)
  Otherwise, will (assoc db topic data)"
  [db query data]
  (let [[topic _] query]
    (if (vector? topic)
      (assoc-in db topic data)
      (assoc db topic data))))

(defn sendf
  "Default send function.
  Doesn't do anything."
  [rem req _]
  (.warn js/console "This send is omitted: " (str req) " -> " (str rem)))

(def ^:private sync
  (atom {:scheduled? false
         :rems nil
         :send (handlers identity sendf)
         :merge (handlers identity mergef)}))

(defn send
  "Sets the send function for the remote.
  Send function is (req, res) -> none
  req is {:reads [query0 query1 ...], :muts [query0 query1 ...]}.
  response must be [[query0 data0], [query1 data1], ...].
  The responsibility of the function will be to pass the req to the remote, and call the res with the response.
  Use :remlok/default to set the fallback.
  Use :remlok/mware to set the middleware."
  [rem f]
  (swap! sync update :send handle rem f))

(defn merge
  "Sets the merge function for the topic.
  Merge function is (db, query, data) -> db*
  Note that the db will be already derefed.
  Use :remlok/default to set the fallback.
  Use :remlok/mware to set the middleware."
  [topic f]
  (swap! sync update :merge handle topic f))

(defn merge!
  "Merges the novelty.
  nov should be [[query0 data0], [query1 data1], ...]."
  [nov]
  (let [hs (get @sync :merge)]
    (swap!
      db
      #(reduce
        (fn [db [[topic _ :as query] data]]
          (let [f (handler hs topic)]
            (f db query data)))
        %1
        %2)
      nov)))

(defn- sync! []
  (let [{:keys [send rems]} @sync]
    (doseq [[rem {:keys [reads muts]}] rems
            :let [req {:reads (distinct reads)
                       :muts muts}
                  send! (handler send rem)]]
      (send! rem req merge!))
    (swap! sync assoc :scheduled? false :rems nil)))

(defn- sched-sync! []
  (let [{:keys [scheduled?]} @sync]
    (when-not scheduled?
      (swap! sync assoc :scheduled? true)
      (js/setTimeout sync! 0))))

(defn- sched-read! [rem query]
  (swap! sync update-in [:rems rem :reads] (fnil conj []) query)
  (sched-sync!))

(defn- sched-mut! [rem query]
  (swap! sync update-in [:rems rem :muts] (fnil conj []) query)
  (sched-sync!))

;;;;;;;;;;;;;;;
;; Pub / Sub ;;
;;;;;;;;;;;;;;;

(defn pubf
  "Default publication function.
  If topic is a vector, will create a reaction of (get-in @db topic)
  Otherwise, will create a reaction of (get @db topic)"
  [db [topic _]]
  {:loc
   (if (vector? topic)
     (reaction
       (get-in @db topic))
     (reaction
       (get @db topic)))})

(defn mutf
  "Default mutation function.
  If topic is a vector, will (assoc-in db topic args)
  Otherwise, will (assoc db topic args)"
  [db [topic args]]
  {:loc
   (if (vector? topic)
     (assoc-in db topic args)
     (assoc db topic args))})

(def ^:private pubs
  (atom
    (handlers identity pubf)))

(def ^:private muts
  (atom
    (handlers identity mutf)))

(defn pub
  "Publishes the topic using the supplied function.
  The function must be (db, query) -> {:loc reaction, :rem-a query-a, :rem-b query-b, ...}
  All map keys are optional.
  Note that the db will not be derefed, so that you can build a reaction.
  Use :remlok/default to set the fallback.
  Use :remlok/mware to set the middleware."
  [topic f]
  (swap! pubs handle topic f))

(defn mut
  "Sets the mutation handler for the topic using the supplied function.
  The function must be (db, query) -> {:loc db*, :rem-a query-a, :rem-b query-b, ...}
  All map keys are optional.
  Note that the db will be already derefed.
  Use :remlok/default to set the fallback.
  Use :remlok/mware to set the middleware."
  [topic f]
  (swap! muts handle topic f))

(defn read
  "Reads the query using the function set by pub.
  The query is a vector of two, [topic args].
  Both topic and args are arbitrary clojure values.
  Returns a reaction."
  [query]
  (let [[topic _] query
        f (handler @pubs topic)
        res (make-shield #(f db query))
        loc (get res :loc ::none)
        rems (dissoc res :loc)]
    (doseq [[rem query] rems
            :when query]
      (sched-read! rem query))
    (if (not= loc ::none)
      loc
      (r/atom nil))))

(defn mut!
  "Performs the mutation using the function set by mut.
  The query is a vector of two, [topic args].
  Both topic and args are arbitrary clojure values.
  Always returns nil."
  [query]
  (let [[topic _] query
        f (handler @muts topic)
        res (make-shield #(f @db query))
        loc (get res :loc ::none)
        rems (dissoc res :loc)]
    (when (not= loc ::none)
      (reset! db loc))
    (doseq [[rem query] rems
            :when query]
      (sched-mut! rem query))
    nil))