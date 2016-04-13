(ns remlok.playground
  (:require
    [remlok.router :as r]))

(defmulti readf r/route)
(defmethod readf :users/guys [{:keys [db] :as ctx} {:keys [query]}]
  (let [join (map
               #(r/read readf (assoc ctx :user %) query)
               (get db :guys))]
    {:loc (mapv :loc join)}))
(defmethod readf :users/girls [_ _]
  {:loc []})
(defmethod readf :user/name [{:keys [user]} _]
  {:loc (get user :name)})
(defmethod readf :user/age [{:keys [user]} _]
  {:loc (get user :age)})
(defmethod readf :default [_ _]
  nil)
(comment
  (def db {:guys [{:name "Bob" :age 27}
                  {:name "Roger" :age 29}]})
  (r/read readf {:db db} [{:users/guys [:user/name]}]))

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