(ns remlok.playground
  (:require
    [remlok.router :as r]))

(comment
  (require '[remlok.router :as r])
  (defmulti readf r/route)
  (defmethod readf :users/guys [{:keys [db] :as ctx} {:keys [query]}]
    (map
      #(r/read readf (assoc ctx :user %) query)
      (get db :guys)))
  (defmethod readf :users/girls [_ _]
    [])
  (defmethod readf :user/name [{:keys [user]} _]
    (get user :name))
  (defmethod readf :user/age [{:keys [user]} _]
    (get user :age))
  (defmethod readf :default [_ _]
    nil)
  (def db {:guys [{:name "Bob" :age 27}
                  {:name "Roger" :age 29}]})
  (r/read readf {:db db} [{:users/guys [:user/name]}])
  (r/read readf {:db db} [{:users/guys [:user/name :user/age]}]))

(comment
  (require '[remlok.router :as r])
  (defmulti readf r/route)
  (defmethod readf :users/guys [{:keys [db rec] :as ctx} {:keys [query]}]
    {:loc (map
            #(rec (assoc ctx :user %) query)
            (get db :guys))})
  (defmethod readf :users/girls [_ _]
    {:loc []})
  (defmethod readf :user/name [{:keys [user]} _]
    {:loc (get user :name)})
  (defmethod readf :user/age [{:keys [user]} _]
    {:loc (get user :age)})
  (defmethod readf :default [_ _]
    nil)
  (def db {:guys [{:name "Bob" :age 27}
                  {:name "Roger" :age 29}]})
  (r/read readf {:db db} [{:users/guys [:user/name]}] false)
  (r/read readf {:db db} [{:users/guys [:user/name :user/age]}] false))