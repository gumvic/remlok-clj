(ns remlok.loc
  (:refer-clojure :exclude [read])
  (:require
    [rum.core :as rum]
    [sablono.core :refer-macros [html]]))

;; TODO when comp mounts, it renders twice (must be handled on rum side)
;; TODO comp name
;; TODO modularize (but how?)

;;;;;;;;;;;;;;;;;;;;
;; React Wrappers ;;
;;;;;;;;;;;;;;;;;;;;

(declare ui-reg! ui-args! ui-unreg! ui-render)

(def ^:private ^:dynamic *app*)

(defn- rum-will-mount
  [{app ::app id :rum/id re-com :rum/react-component args ::args com ::com :as st}]
  (let [render! #(rum/request-render re-com)
        com* (assoc com :render! render!)]
    (com-reg! app id com*)
    (com-args! app id args))
  st)

(defn- rum-should-update
  [{old-args ::args} {new-args ::args}]
  (not= old-args new-args))

(defn- rum-transfer-state
  [_ {app ::app id :rum/id args ::args :as st}]
  (com-args! app id args)
  st)

(defn- rum-render
  [{app ::app id :rum/id :as st}]
  (let [vdom (binding [*app* app]
               (html
                 (com-render app id)))]
    [vdom st]))

(defn- rum-will-unmount
  [{app ::app id :rum/id :as st}]
  (com-unreg! app id)
  st)

(def ^:private rum-mixin
  {:will-mount rum-will-mount
   :should-update rum-should-update
   :transfer-state rum-transfer-state
   :render rum-render
   :will-unmount rum-will-unmount})

(defn- rum-com [com]
  (let [class (rum/build-class [rum-mixin] "anonymous")]
    (fn
      ([]
       (rum/element
         class
         {::args nil ::app *app* ::com com}))
      ([args]
       (rum/element
         class
         {::args args ::app *app* ::com com})))))

(defn ui [ui]
  (rum-com ui))

;;;;;;;;;;;;;;
;; UI State ;;
;;;;;;;;;;;;;;

(defn- ui-state [app id]
  (let [{:keys [state]} app]
    (get-in @state [:remlok/ui :remlok/ui->state id])))

(defn- ui-swap-state! [app id f]
  (let [{:keys [state]} app]
    (vswap! state update-in [:remlok/ui :remlok/ui->state id] f)))

(defn- ui-reset-state! [app id st]
  (let [{:keys [state]} app]
    (vswap! state update-in [:remlok/ui :remlok/ui->state id] st)))

(defn- ui-reg! [app id])

(defn- ui-args! [app id])

(defn- ui-unreg! [app id])

(defn- ui-render [app id])

(defn- ui-render! [app id]
  )

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