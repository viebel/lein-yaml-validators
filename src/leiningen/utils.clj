(ns leiningen.utils)

(defn branches-and-leaves [m]
  (as-> (tree-seq coll? #(if (map? %) (vals %) %) m) $
        (group-by coll? $)
        (assoc $ true (or (get $ true) []))
        (assoc $ false (or (get $ false) []))
        (clojure.set/rename-keys $ {true :branches false :leaves})))

(defn filter-branches [m p]
  (->> (branches-and-leaves m)
       :branches
      (filter p)))