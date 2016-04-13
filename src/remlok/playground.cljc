(ns remlok.playground
  (:require
    [remlok.router :as r]))

(comment
  (require '[remlok.router :as r])
  (defmulti readf r/route)
  (defmethod readf :default [_ _]
    nil)
  (defmethod readf :foo [_ _]
    {:loc :foo})
  (defmethod readf :bar [_ _]
    {:loc :bar
     :rem [:bar]})
  (defmethod readf :baz [ctx {:keys [query]}]
    (let [{:keys [loc rem]} (r/read readf ctx query)
          rem (when rem [{:baz rem}])]
      {:loc loc :rem rem}))
  (r/read readf nil [:foo])
  (r/read readf nil [:foo :bar :baz])
  (r/read readf nil [{:baz [:foo :bar :zooz]}]))