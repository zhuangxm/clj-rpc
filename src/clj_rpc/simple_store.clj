(ns clj-rpc.simple-store
  (:use [ring.middleware.session.store]))

;;use a atom of atom data struct to implement a ring SessionStore
;;db data is a atom of map and every value in the map is also a atom.
;;like (atom {key1 (atom data1) key2 (atom data2)})

;;TODO use priority-map data struct to support index
(deftype SimpleStore [session-map]
  SessionStore
  (read-session [_ key]
    (when-let [atom-value (get @session-map key)]
      @atom-value))
  (write-session [_ key data]
    (if-let [atom-value (get @session-map key)]
      (reset! atom-value data)
      (swap! session-map assoc key (atom data))))
  (delete-session [_ key]
    (swap! session-map dissoc key)
    nil))

(defn simple-store
  "Creates an in-memory session storage engine."
  ([] (simple-store (atom {})))
  ([session-atom] (SimpleStore. session-atom)))
