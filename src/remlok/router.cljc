(ns remlok.router
  (:require
    [remlok.query :as q]))

(defn- route* [node]
  (let [{:keys [attr fun]} (q/node->ast node)]
    (if fun
      `(~fun ~attr)
      attr)))

(defn route
  ([_ node]
   (route* node))
  ([_ node _]
   (route* node)))