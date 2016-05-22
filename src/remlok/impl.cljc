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
        def (get handlers :remlok/default)
        handler (get handlers topic def)]
    (mware handler)))