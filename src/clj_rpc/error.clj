(ns clj-rpc.error)

(defn raise-error
  [code msg]
  (throw (clj_rpc.Exception. code msg)))
