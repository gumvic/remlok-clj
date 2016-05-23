(ns remlok.impl)

;;;;;;;;;;;;;;;;;;;;;;;;
;; Per Topic Handlers ;;
;;;;;;;;;;;;;;;;;;;;;;;;

(defn handlers [mware default]
  {:remlok/mware mware
   :remlok/default default})

(defn handle [handlers topic handler]
  (assoc handlers topic handler))

(defn handler [handlers topic]
  (let [mware (get handlers :remlok/mware)
        default (get handlers :remlok/default)
        handler (get handlers topic default)]
    (mware handler)))