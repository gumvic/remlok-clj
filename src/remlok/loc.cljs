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
;; UI State ;;
;;;;;;;;;;;;;;

(defn- ui-state [app id]
  (let [{:keys [state]} app]
    (get-in @state [:remlok/ui :ui->state id])))

(defn- ui-swap! [app id f]
  (let [{:keys [state]} app]
    (vswap! state update-in [:remlok/ui :ui->state id] f)))

(defn- ui-reset! [app id st]
  (let [{:keys [state]} app]
    (vswap! state update-in [:remlok/ui :ui->state id] st)))

(defn- ui-forget! [app id]
  (let [{:keys [state]} app]
    (vswap! state update-in [:remlok/ui :ui->state] dissoc id)))

(defn- ui-sub! [app id attr]
  (let [{:keys [state]} app]
    (vswap! state update-in [:remlok/ui :attr->ui attr] (fnil conj #{}) id)))

(defn- ui-unsub! [app id attr]
  (let [{:keys [state]} app]
    (vswap! state update-in [:remlok/ui :attr->ui attr] disj id)
    (when-not (seq
                (get-in @state [:remlok/ui :attr->ui attr]))
      (vswap! state update-in [:remlok/ui :attr->ui] dissoc attr))))

(defn- ui-reg! [app id ui]
  (ui-reset! app id ui))

(defn- query+args->ast [query args]
  )

(defn- ast->attrs [ast]
  )

(defn- ui-args! [app id args]
  (let [{:keys [query attrs]} (ui-state app id)
        ast (when query
              (query+args->ast query args))
        attrs* (ast->attrs ast)]
    (doseq [attr attrs]
      (ui-unsub! app id attr))
    (doseq [attr attrs*]
      (ui-sub! app id attr))
    (ui-swap! app id #(assoc % :ast ast :attrs attrs*))))

(defn- ui-unreg! [app id]
  (let [{:keys [attrs]} (ui-state app id)]
    (doseq [attr attrs]
      (ui-unsub! app id attr))
    (ui-forget! app id)))

(defn- ui-render [app id]
  (let [{:keys [ast render]} (ui-state app id)]
    ))

(defn- ui-render! [app id]
  (let [{:keys [render!]} (ui-state app id)]
    (render!)))

;;

(defn- read [app query])

(defn- mut! [app query])

;;;;;;;;;
;; App ;;
;;;;;;;;;

(def ^:private def-funs
  {})

(defn app [funs]
  {:state (volatile!
            {:remlok/ui
             {:attr->ui {}
              :ui->state {}}
             :remlok/db {}})
   :funs (merge def-funs funs)})