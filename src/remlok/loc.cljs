(ns remlok.loc
  (:refer-clojure :exclude [read])
  (:require
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

(defn ui [ui]
  (rum-com ui))

;;;;;;;;;;;;;;
;; Read/Mut ;;
;;;;;;;;;;;;;;

(defn- read-loc* [readf ctx ast]
  (not-empty
    (into
      {}
      (comp
        (map #(when-let [r (get (readf ctx %) :loc)]
               [(get % :attr) r]))
        (filter some?))
      ast)))

(defn- read-loc [f ctx ast]
  (let [f* #(read-loc* f %1 %2)
        ctx* (assoc ctx :read f*)]
    (f* ctx* ast)))

(defn- read-rem* [readf ctx ast]
  (not-empty
    (into
      []
      (comp
        (map #(get (readf ctx %) :rem))
        (filter some?))
      ast)))

(defn- read-rem [f ctx ast]
  (let [f* #(read-rem* f %1 %2)
        ctx* (assoc ctx :read f*)]
    (f* ctx* ast)))

(defn- mut-loc* [mutf ctx ast]
  (let [fs (into
             []
             (comp
               (map #(get (mutf ctx %) :loc))
               (filter some?))
             ast)]
    (fn []
      (doseq [f fs]
        (f)))))

(defn- mut-loc [f ctx ast]
  (let [f* #(mut-loc* f %1 %2)
        ctx* (assoc ctx :mut f*)]
    (f* ctx* ast)))

(defn- mut-rem* [mutf ctx ast]
  (not-empty
    (into
      []
      (comp
        (map #(get (mutf ctx %) :rem))
        (filter some?))
      ast)))

(defn- mut-rem [f ctx ast]
  (let [f* #(mut-rem* f %1 %2)
        ctx* (assoc ctx :mut f*)]
    (f* ctx* ast)))

;;;;;;;;;;;;;;;;;;;
;; Query Helpers ;;
;;;;;;;;;;;;;;;;;;;

(defn- ast+args->ast [ast args]
  ast)

(defn- ast->attrs [ast]
  )

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

(defn- ui-sync! [app id]
  (let [{:keys [state]} app
        {:keys [db readf]} @state
        {:keys [ast]} (ui-state app id)
        ctx {:db db}
        loc (read-loc readf ctx ast)
        rem (q/query->ast
              (read-rem readf ctx ast))]
    (when (seq rem)
      (schedule-read! app rem))
    (ui-swap! app id #(assoc % :loc loc :rem rem))))

(defn- ui-sync-fat! [app id]
  (let [{:keys [state]} app
        {:keys [readf]} @state
        {:keys [ast]} (ui-state app id)
        ctx {:db nil}
        rem (q/query->ast
              (read-rem readf ctx ast))]
    (when (seq rem)
      (schedule-read! app rem))))

(defn- ui-reg! [app id ui]
  (let [{:keys [query render render!]} ui
        ast (q/query->ast query)
        attrs (ast->attrs ast)
        st {:ast ast
            :attrs attrs
            :render render
            :render! render!}]
    (doseq [attr attrs]
      (ui-sub! app id attr))
    (ui-reset! app id st)))

(defn- ui-args! [app id args]
  (let [{:keys [ast]} (ui-state app id)
        ast (ast+args->ast ast args)]
    (ui-swap! app id #(assoc % :ast ast))
    (ui-sync! app id)))

(defn- ui-unreg! [app id]
  (let [{:keys [attrs]} (ui-state app id)]
    (doseq [attr attrs]
      (ui-unsub! app id attr))
    (ui-forget! app id)))

(defn- ui-render [app id]
  (let [{:keys [loc render]} (ui-state app id)]
    (render loc)))

(defn- ui-render! [app id]
  (let [{:keys [render!]} (ui-state app id)]
    (render!)))

;;;;;;;;;;
;; Sync ;;
;;;;;;;;;;

(defn- sync! [app]
  (let [{:keys [state]} app
        {{:keys [reads muts]} :sync syncf :syncf} @state
        sync (merge
               (when (seq reads) {:reads reads})
               (when (seq muts) {:muts muts}))]
    (when (seq sync)
      (syncf sync))
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

(defn- schedule-read! [app ast]
  (let [{:keys [state]} app]
    (vswap! state update-in [:sync :reads] conj ast)
    (schedule-sync! app)))

(defn- schedule-mut! [app ast]
  (let [{:keys [state]} app]
    (vswap! state update-in [:sync :muts] conj ast)
    (schedule-sync! app)))

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
   :syncf (fn [_])})

(defn app [state]
  {:state
   (volatile!
     (merge def-state state))})

(defn mount! [app com el]
  (binding [*app* app]
    (rum/mount
      (com)
      el)))