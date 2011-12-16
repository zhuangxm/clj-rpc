(ns clj-rpc.simple-db)

;;simple memory key value data
;;db data is a atom of map and every value in the map is also a atom.
;;like (atom {key1 (atom data1) key2 (atom data2)})

;;TODO use priority-map data struct to support index
(defn create-db
  []
  (atom {}))

(defn get-data
  [db k]
  (when-let [atom-value (get @db k)]
    @atom-value))

(defn save-data
  [db k value]
  (if-let [atom-value (get @db k)]
    (reset! atom-value value)
    (swap! db assoc k (atom value))))

(defn delete-data
  [db k]
  (swap! db dissoc k))

(defn data-count
  [db]
  (count @db))
