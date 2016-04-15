(ns remlok.playground
  (:require
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