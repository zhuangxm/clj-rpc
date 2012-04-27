(ns clj-rpc.error)

(defn raise-error
  ([code msg]
     (throw (clj_rpc.Exception. code msg)))
  ([code data msg]
      (throw (clj_rpc.Exception. code data msg))))
