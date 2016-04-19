(ns remlok.router
  (:require
    [remlok.query :as q]))

(defn route [_ node]
  (let [{:keys [attr fun]} (q/node->ast node)]
    (if fun
      `(~fun ~attr)
      attr)))