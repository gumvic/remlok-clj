(ns remlok.impl)

;;;;;;;;;;;;;
;; Helpers ;;
;;;;;;;;;;;;;

#_(defn select-fun [fs [topic _]]
  (let [df (get fs :remlok/default)
        f (get fs topic df)
        mware (get fs :remlok/mware)]
    (mware f)))

(defn- select-fun [fs [topic _]]
  (get fs topic (get fs :remlok/default)))