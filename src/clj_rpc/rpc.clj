(ns clj-rpc.rpc
  (:require [clj-rpc.error])
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
  [code id & [msg data]]
  {:error {:code (or (error-codes code) code)
           :message (or msg (error-msgs code))
           :data (or data "")}
   :id id})

(defn mk-error-from-exinfo
  [id ^clojure.lang.ExceptionInfo e]
  (let [msg (.getMessage e)
        [{:keys [code data]}] (ex-data e)]
    (mk-error code id msg data)))

(defn execute-method
  "execute a function f  with params
   return rpc response
   id : used for generate the response"
  [f {:keys [params id error]}]
  (try
    (if error
      (mk-error (:code error) id (:message error))
      (if f 
        (mk-response (apply f params) id)
        (mk-error :method-not-found id)))
    (catch ArityException e (mk-error :invalid-params id (.getMessage e)))
    (catch clojure.lang.ExceptionInfo e (mk-error-from-exinfo id e))
    (catch Exception e (mk-error :internal-error id (.getMessage e)))))
