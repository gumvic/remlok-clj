(ns remlok.loc
  (:refer-clojure :exclude [read])
  (:require
    [clojure.zip :as zip]
    [rum.core :as rum]
    [sablono.core :refer-macros [html]]
    [remlok.query :as q]))

;; TODO when comp mounts, it renders twice (must be handled on rum side)
;; TODO comp name
;; TODO modularize (but how?)

;;;;;;;;;;;;;;;;;;;;
;; React Wrappers ;;
;;;;;;;;;;;;;;;;;;;;

(declare ui-reg! ui-args! ui-unreg! ui-render)

(def ^:private ^:dynamic *app*)

(defn- rum-will-mount
  [{app ::app id :rum/id re-com :rum/react-component args ::args ui ::ui :as st}]
  (let [render! #(rum/request-render re-com)
        ui* (assoc ui :render! render!)]
    (ui-reg! app id ui*)
    (ui-args! app id args))
  st)

(defn- rum-should-update
  [{old-args ::args} {new-args ::args}]
  (not= old-args new-args))

(defn- rum-transfer-state
  [_ {app ::app id :rum/id args ::args :as st}]
  (ui-args! app id args)
  st)

(defn- rum-render
  [{app ::app id :rum/id :as st}]
  (let [vdom (binding [*app* app]
               (html
                 (ui-render app id)))]
    [vdom st]))

(defn- rum-will-unmount
  [{app ::app id :rum/id :as st}]
  (ui-unreg! app id)
  st)

(def ^:private rum-mixin
  {:will-mount rum-will-mount
   :should-update rum-should-update
   :transfer-state rum-transfer-state
   :render rum-render
   :will-unmount rum-will-unmount})

(defn- rum-com [ui]
  (let [class (rum/build-class [rum-mixin] "anonymous")]
    (fn
      ([]
       (rum/element
         class
         {::args nil ::app *app* ::ui ui}))
      ([args]
       (rum/element
         class
         {::args args ::app *app* ::ui ui})))))

;;;;;;;;;;;;;;;;;;;
;; Query Helpers ;;
;;;;;;;;;;;;;;;;;;;

(defn- query-zip [query]
  (zip/zipper
    #(or (vector? %) (map? %) (list? %))
    seq
    (fn [node children]
      (cond
        (vector? node) (vec children)
        (map? node) (into {} children)
        (seq? node) children))
    query))

(defn- arg? [x]
  (and
    (symbol? x)
    (= (first (name x)) \?)))

(defn- arg->keyword [arg]
  (keyword
    (subs (name arg) 1)))

;; TODO only replace in args
(defn- query+args [query args]
  (loop [loc (query-zip query)]
    (if (zip/end? loc)
      (zip/root loc)
      (let [node (zip/node loc)
            loc* (if (arg? node)
                   (zip/replace loc (get args (arg->keyword node)))
                   loc)]
        (recur (zip/next loc*))))))

(defn- query->attrs [query]
  (distinct
    (reduce
      (fn [attrs node]
        (if (q/join? node)
          (into attrs (query->attrs (q/subq node)))
          (conj attrs (q/attr node))))
      []
      (q/nodes query))))

(comment
  (pprint
    (arg-paths
      (q/query->ast '[(:foo ?bar)
                      {:baz [(limit :bax {:from 0 :to ?to})]}])))
  (let [q '[(:foo ?bar)
            {:baz [(limit :bax {:from 0 :to ?to})]}]
        ast (q/query->ast q)
        paths (arg-paths ast)
        args {:to 10}]
    (pprint
      (q/ast->query
        (ast->ast* ast paths args)))))

;;;;;;;;
;; UI ;;
;;;;;;;;

(defn ui [ui]
  (rum-com ui))

;;;;;;;;;;;;;;
;; Read/Mut ;;
;;;;;;;;;;;;;;

(defn- read-loc* [readf ctx query]
  (not-empty
    (into
      {}
      (comp
        (map #(when-let [r (get (readf ctx %) :loc)]
               [(q/attr %) r]))
        (filter some?))
      (q/nodes query))))

(defn- read-loc [f ctx query]
  (let [f* #(read-loc* f %1 %2)
        ctx* (assoc ctx :read f*)]
    (f* ctx* query)))

(defn- read-rem* [readf ctx query]
  (not-empty
    (into
      []
      (comp
        (map #(get (readf ctx %) :rem))
        (filter some?))
      (q/nodes query))))

(defn- read-rem [f ctx query]
  (let [f* #(read-rem* f %1 %2)
        ctx* (assoc ctx :read f*)]
    (f* ctx* query)))

#_(defn- mut-loc* [mutf ctx query]
  (let [fs (into
             []
             (comp
               (map #(get (mutf ctx %) :loc))
               (filter some?))
             (q/nodes query))]
    (fn [db]
      (reduce #(%2 %1) db fs))))

#_(defn- mut-loc* [mutf ctx query]
  (let [loc (into
              []
              (comp
                (map #(get (mutf ctx %) :loc))
                (filter some?))
              (q/nodes query))
        actions (into [] (comp (map :action) (filter some?)) loc)
        action (fn [db]
              (reduce #(%2 %1) db actions))
        attrs (into [] (comp (map :attrs) (filter some?)) loc)]
    {:act action
     :attrs attrs}))

(defn- mut-loc* [mutf ctx query]
  (let [loc (reduce
              (fn [{actions :actions attrs :attrs}
                   {action* :action attrs* :attrs :as foo}]
                {:actions (conj actions (or action* identity))
                 :attrs (into attrs attrs*)})
              {:actions []
               :attrs []}
              (map
                #(get (mutf ctx %) :loc)
                (q/nodes query)))
        attrs (get loc :attrs)
        action (fn [db] (reduce #(%2 %1) db (get loc :actions)))]
    {:action action
     :attrs attrs}))

(defn- mut-loc [f ctx query]
  (mut-loc* f ctx query))

(defn- mut-rem* [mutf ctx query]
  (not-empty
    (into
      []
      (comp
        (map #(get (mutf ctx %) :rem))
        (filter some?))
      (q/nodes query))))

(defn- mut-rem [f ctx query]
  (mut-rem* f ctx query))

;;;;;;;;;;;;;;
;; UI State ;;
;;;;;;;;;;;;;;

(declare schedule-read! schedule-mut!)

(defn- ui-state [app id]
  (let [{:keys [state]} app]
    (get-in @state [:ui :ui->state id])))

(defn- ui-swap! [app id f]
  (let [{:keys [state]} app]
    (vswap! state update-in [:ui :ui->state id] f)))

(defn- ui-reset! [app id st]
  (let [{:keys [state]} app]
    (vswap! state assoc-in [:ui :ui->state id] st)))

(defn- ui-forget! [app id]
  (let [{:keys [state]} app]
    (vswap! state update-in [:ui :ui->state] dissoc id)))

(defn- ui-sub! [app id attr]
  (let [{:keys [state]} app]
    (vswap! state update-in [:ui :attr->ui attr] (fnil conj #{}) id)))

(defn- ui-unsub! [app id attr]
  (let [{:keys [state]} app]
    (vswap! state update-in [:ui :attr->ui attr] disj id)
    (when-not (seq
                (get-in @state [:ui :attr->ui attr]))
      (vswap! state update-in [:ui :attr->ui] dissoc attr))))

(defn- ui-by-attr [app attr]
  (let [{:keys [state]} app]
    (get-in @state [:ui :attr->ui attr])))

#_(defn- ui-sync! [app id]
  (let [{:keys [state]} app
        {:keys [db readf]} @state
        {:keys [query*]} (ui-state app id)
        ctx {:db db}
        loc (read-loc readf ctx query*)
        rem (read-rem readf ctx query*)]
    (schedule-read! app rem)
    (ui-swap! app id #(assoc % :loc loc :rem rem))))

(defn- ui-sync-loc! [app id]
  (let [{:keys [state]} app
        {:keys [db readf]} @state
        {:keys [query*]} (ui-state app id)
        loc (read-loc readf {:db db} query*)]
    (ui-swap! app id #(assoc % :loc loc))))

(defn- ui-sync-rem! [app id]
  (let [{:keys [state]} app
        {:keys [db readf]} @state
        {:keys [query*]} (ui-state app id)
        rem (read-rem readf {:db db} query*)]
    (schedule-read! app rem)))

;; TODO narrow rem to query
(defn- ui-sync-fat! [app id query]
  (let [{:keys [state]} app
        {:keys [readf]} @state
        {:keys [query*]} (ui-state app id)
        ctx {:db nil}
        rem (read-rem readf ctx query*)]
    (schedule-read! app rem)))

(defn- ui-sync! [app id]
  (ui-sync-loc! app id)
  (ui-sync-rem! app id))

(defn- ui-reg! [app id st]
  (let [{:keys [query]} st
        attrs (query->attrs query)]
    (doseq [attr attrs]
      (ui-sub! app id attr))
    (ui-reset! app id st)))

(defn- ui-args! [app id args]
  (let [{query :query old-args :args} (ui-state app id)
        args (merge old-args args)
        query* (query+args query args)]
    (ui-swap! app id #(assoc % :query* query*))
    (ui-sync! app id)))

(defn- ui-unreg! [app id]
  (let [{:keys [attrs]} (ui-state app id)]
    (doseq [attr attrs]
      (ui-unsub! app id attr))
    (ui-forget! app id)))

(defn- ui-render [app id]
  (let [{:keys [loc render]} (ui-state app id)
        ui {:app app :id id}]
    (render loc ui)))

(defn- ui-render! [app id]
  (let [{:keys [render!]} (ui-state app id)]
    (render!)))

(defn args! [ui args]
  (let [{:keys [app id]} ui]
    (ui-args! app id args)
    (ui-render! app id)))

(defn mut! [ui query]
  (let [{:keys [app]} ui
        {:keys [state]} app
        {:keys [mutf db]} @state
        ctx {:db db}
        {:keys [action attrs]} (mut-loc mutf ctx query)
        rem (mut-rem mutf ctx query)]
    (schedule-mut! app rem)
    (vswap! state update :db action)
    (let [ids (distinct (mapcat #(ui-by-attr app %) attrs))]
      (doseq [id ids]
        (ui-sync-loc! app id)
        ;;(ui-sync-fat! app id)
        (ui-render! app id)))))

;;;;;;;;;;
;; Sync ;;
;;;;;;;;;;

(defn merge! [app query tree]
  (let [{:keys [state]} app
        {:keys [normf mergef]} @state
        attrs (query->attrs query)
        db* (normf tree)]
    (vswap! state update :db mergef db*)
    (doseq [attr attrs]
      )))

(defn- sync! [app]
  (let [{:keys [state]} app
        {{:keys [reads muts]} :sync
         syncf :syncf} @state
        sync (merge
               (when (seq reads) {:reads reads})
               (when (seq muts) {:muts muts}))]
    (when (seq sync)
      (syncf
        sync
        #(doseq [[query tree] %]
          (merge! app query tree))))
    (vswap! state assoc :sync {:scheduled? false
                               :reads []
                               :muts []})))

;; TODO use goog nextTick
(defn- schedule-sync! [app]
  (let [{:keys [sync]} app
        {:keys [scheduled?]} @sync]
    (when-not scheduled?
      (vswap! sync assoc :scheduled? true)
      (js/setTimeout
        #(sync! app)
        0))))

(defn- schedule-read! [app query]
  (when (seq (q/nodes query))
    (let [{:keys [state]} app]
      (vswap! state update-in [:sync :reads] conj query)
      (schedule-sync! app))))

(defn- schedule-mut! [app query]
  (when (seq (q/nodes query))
    (let [{:keys [state]} app]
      (vswap! state update-in [:sync :muts] conj query)
      (schedule-sync! app))))

;;;;;;;;;
;; App ;;
;;;;;;;;;

(def ^:private def-state
  {:ui {:attr->ui {}
        :ui->state {}}
   :db nil
   :sync {:scheduled? false
          :reads []
          :muts []}
   :readf (fn [_ _])
   :mutf (fn [_ _])
   :syncf (fn [_])
   :normf identity
   :mergef merge})

(defn app [state]
  {:state
   (volatile!
     (merge def-state state))})

(defn mount! [app com el]
  (binding [*app* app]
    (rum/mount
      (com)
      el)))