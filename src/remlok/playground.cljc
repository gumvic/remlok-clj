(ns remlok.playground
  (:require
    [remlok.query :as q]
    [remlok.router :as r]))

(comment
  (require '[remlok.router :as r])
  (defmulti readf r/route)
  (defn- read-one [cur {:keys [read] :as ctx} {:keys [attr fun args query]}]
    {:loc (if query
            (read (assoc ctx :prev cur))
            cur)
     :rem (if query
            (when-let [rem (read (assoc ctx :prev cur) query)]
              {attr rem}))})
  (defn- read-many [cur {:keys [read] :as ctx} {:keys [attr fun args query]}]
    {:loc (if query
            (mapv #(read (assoc ctx :prev %) query) cur)
            cur)
     :rem (when query
            (when-let [rem (not-empty
                             (into
                               []
                               (comp
                                 (mapcat #(read (assoc ctx :prev %) query)))
                               cur))]
              {attr rem}))})
  (defmethod readf :default [{:keys [db] :as ctx} {:keys [attr] :as ast}]
    (let [prev (if (contains? ctx :prev)
                 (get ctx :prev)
                 db)
          cur (get prev attr)]
      (if (vector? cur)
        (read-many cur ctx ast)
        (read-one cur ctx ast))))
  (def db {:users/all
           [{:user/name "Bob" :user/age 27}
            {:user/name "Roger" :user/age 29}
            {:user/name "Alice"}]})
  (r/read readf {:db db} [{:users/all [:user/name :user/age]}]))

(comment
  (require '[remlok.query :as q])
  (require '[remlok.router :as r])
  (defmulti readf r/route)
  (defmethod readf :users [{:keys [db read] :as ctx} {:keys [query]}]
    {:loc (mapv
            #(read (assoc ctx :user (second %)) query)
            (get @db :users))})
  (defmethod readf :user/name [{:keys [user]} _]
    {:loc (get user :user/name)})
  (defmethod readf :user/age [{:keys [user]} _]
    {:loc (get user :user/age)})
  (defmethod readf :default [_ _]
    nil)
  (defmulti mutf r/route)
  (defmethod mutf :user/ages! [{:keys [db]} {:keys [args]}]
    {:loc (fn [] (vswap! db update-in [:users args :user/age] inc))})
  (def db
    (volatile!
      {:users
       {1 {:db/id 1 :user/name "Bob" :user/age 27}
        2 {:db/id 2 :user/name "Roger" :user/age 29}
        3 {:db/id 3 :user/name "Alice" :user/age 25}}}))
  (r/read readf {:db db} (q/compile [{:users [:user/name :user/age]}]))
  ((:loc (r/mut mutf {:db db} (q/compile '[(:user/ages! 1)]))))
  (r/read readf {:db db} (q/compile [{:users [:user/name :user/age]}])))