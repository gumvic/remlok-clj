(ns remlok.loc
  (:require
    [rum.core :as rum]
    [sablono.core :refer-macros [html]]
    [remlok.exec :as exe]
    [remlok.loc.impl.query :refer [query+args query+attrs query->attrs]]))

;; TODO comp name
;; TODO modularize (but how?)
;; TODO upon mounting, comp renders twice
;; TODO better name for fat->query

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

;;;;;;;;
;; UI ;;
;;;;;;;;

(defn ui [ui]
  (rum-com ui))

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

(defn- ui-by-attrs [app attrs]
  (distinct
    (mapcat
      #(ui-by-attr app %)
      attrs)))

(defn- ui-sync-loc! [app id]
  (let [{:keys [state]} app
        {:keys [readf]} @state
        {:keys [query* render!]} (ui-state app id)
        ctx {:st state}
        loc (exe/read readf ctx query*)]
    (ui-swap! app id #(assoc % :loc loc))
    (render!)))

(defn- ui-sync-rem! [app id]
  (let [{:keys [state]} app
        {:keys [readf]} @state
        {:keys [query*]} (ui-state app id)
        ctx {:st state}
        rem (exe/read readf ctx query* :rem)]
    (schedule-read! app rem)))

(defn- ui-sync-fat! [app id attrs]
  (let [{:keys [state]} app
        {:keys [db readf]} @state
        {:keys [query*]} (ui-state app id)
        ctx {:st (vswap! state assoc :db nil)}
        rem (query+attrs
              (exe/read readf ctx query* :rem)
              attrs)]
    (vswap! state assoc :db db)
    (schedule-read! app rem)))

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
    (ui-sync-loc! app id)
    (ui-sync-rem! app id)))

(defn- ui-unreg! [app id]
  (let [{:keys [attrs]} (ui-state app id)]
    (doseq [attr attrs]
      (ui-unsub! app id attr))
    (ui-forget! app id)))

(defn- ui-render [app id]
  (let [{:keys [loc render]} (ui-state app id)
        ui {:app app :id id}]
    (render loc ui)))

(defn args! [ui args]
  (let [{:keys [app id]} ui]
    (ui-args! app id args)))

(defn mut! [ui query]
  (let [{:keys [app]} ui
        {:keys [state]} app
        {:keys [mutf]} @state
        ctx {:st state}
        action! (exe/mut mutf ctx query)
        rem (exe/mut mutf ctx query :rem)]
    (schedule-mut! app rem)
    (let [attrs (action!)]
      (doseq [id (ui-by-attrs app attrs)]
        (ui-sync-loc! app id)
        (ui-sync-fat! app id attrs)))))

;;;;;;;;;;
;; Sync ;;
;;;;;;;;;;

(defn merge! [app query tree]
  (let [{:keys [state]} app
        {:keys [normf mergef]} @state
        attrs (query->attrs query)
        db* (normf tree)]
    (vswap! state update :db mergef db*)
    (doseq [id (ui-by-attrs app attrs)]
      (ui-sync-loc! app id))))

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
  (when (seq query)
    (let [{:keys [state]} app]
      (vswap! state update-in [:sync :reads] conj query)
      (schedule-sync! app))))

(defn- schedule-mut! [app query]
  (when (seq query)
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