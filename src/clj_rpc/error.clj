(ns clj-rpc.error)

(defn raise-error
  [code msg & [data]]
  (throw (ex-info msg {:code code :data data})))
