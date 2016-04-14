(ns remlok.playground
  (:require
    [remlok.router :as r]))

(comment
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
    (r/read readf {:db db} [{:users/guys [:user/name]}])))

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
(comment
  (def db {:guys [{:name "Bob" :age 27}
                  {:name "Roger" :age 29}]})
  (r/read readf {:db db} [{:users/guys [:user/name]}]))