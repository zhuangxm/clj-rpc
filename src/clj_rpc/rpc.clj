(ns clj-rpc.rpc
  (:import [clojure.lang ArityException]))

;;defind rpc message format, refering json-rpc definition

;;error-msg and error-codes original come from
;;https://github.com/matiaslb/jsonrpc4c
;;thanks matiaslb
(def error-msgs
  {:parse-error      "parse protocol error"
   :invalid-request  "an invalid Request object."
   :method-not-found "The method does not exist / is not available."
   :invalid-params   "Invalid method parameter(s)."
   :internal-error   "Internal error."
   :unauthorized     "unauthorized user or action"
   :undefine         "undefine error, please check the server code"})

(def error-codes
  {:parse-error      -32700
   :invalid-request  -32600
   :method-not-found -32601
   :invalid-params   -32602
   :internal-error   -32603
   :unauthorized     401
   :undefine         404})


(defn mk-response
  "generate response"
  [result id]
  {:result result :id id})

(defn mk-error
  "generate error message"
  [code id &[msg data]]
  (let [code (if (get error-codes code) code :undefine)]
    {:error {:code (error-codes code)
             :message (or msg (error-msgs code))
             :data (or data "")}
     :id id}))

(defn execute-method
  "execute a function f  with params
   return rpc response
   id : used for generate the response"
  [f params id]
  (try
    (if f 
      (mk-response (apply f params) id)
      (mk-error :method-not-found id))
    (catch ArityException e (mk-error :invalid-params id (.getMessage e)) )
    (catch Exception e (mk-error :internal-error id (.getMessage e)))))
