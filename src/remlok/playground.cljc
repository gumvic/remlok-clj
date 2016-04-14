(ns remlok.playground
  (:require
    [remlok.router :as r]))

(comment
  (require '[remlok.router :as r])
  (defmulti readf r/route)
  (defmethod readf :users/all [{:keys [db read] :as ctx} {:keys [query]}]
    {:loc (not-empty
            (mapv
              #(read (assoc ctx :user %) query)
              (get db :people)))})
  (defmethod readf :user/name [{:keys [user]} _]
    {:loc (get user :name)})
  (defmethod readf :user/age [{:keys [user]} _]
    {:loc (get user :age)})
  (defmethod readf :default [_ _]
    nil)
  (def db {:people
           [{:name "Bob" :age 27}
            {:name "Roger" :age 29}
            {:name "Alice"}]})
  ;; loc
  (r/read readf {:db db} [{:users/all [:user/name :user/age]}]))