;; Client interface for clj-rpc
;;
;; examples usage:
;; (def endp (rpc-endpoint :server "localhost" :port 8080 :on-wire
;;"json"))
;; where on-wire is the serialization method for wire format of RPC.
;; Currently, 2 wire formats supported: clj (default), json
;;
;; (help endp)
;; to get the list of the functions that the endpoint exported.
;;
;; (invoke-rpc endp "+" 25 12 54)
;; to invoke a remote function + with parameters
;;
;; TODO asynchronizing call of rpc method

;; TODO the implmentation of multi invoke is no so good.
;; TODO just make it work.

(ns clj-rpc.client
  (:require [clj-http.client :as http]
            [clj-http.util :as util]
            [clj-rpc [wire-format :as protocol] [server :as server]]
            [clojure.tools.logging :as logging]))

(defprotocol RpcEndpoint
  (invoke [endpoint func-name args]
    "Invoke functions with name func-name with arguments args on endpoint,
     notice because protocols do not support varargs, a more convinient way
     is using invoke-rpc function instead.")
  (multi-invoke [endpoint invokes]
     "Invoke mutli functions with name func-name with arguments args on endpoint,
     the invokes must a collection of func-name args like [fun1 arg1 fun2 arg2]
     notice because protocols do not support varargs, a more convinient way
     is using multi-invoke-rpc function instead.")
  (help [endpoint]
    "Returns the list of the functions that endpoint support."))

(def ^:private content-type "charset=UTF-8")

(defn- mk-query-body
  "make http invoke body.
   the body has two kinds
   1. a map means a single invoke.
   2. a collection means multi invokes."
  [method-name args & method-args]
  (if (seq method-args)
    (map (fn [[meth ps]] {:method meth :params ps})
         (partition 2 2 (concat [method-name args] method-args)))
    {:method method-name :params args}))

(defn- mk-query
  "make query object to send to endpoint."
  [f-encode method-name args & method-args]
  (let [body (f-encode (apply mk-query-body method-name args method-args)) ]
    {:body body :content-type content-type}))

(defn get-response-value
  "get response key value ,either keywork or string"
  [m key]
  (or (get m key) (get m (name key))))

(defn- get-single-invoke-result
  "get the response result, the response must be a map"
  [response-m]
  (let [error (get-response-value response-m :error)]
    (if (not error)
      (get-response-value response-m :result)
      (throw (RuntimeException. (str  "error cdoe : "
                                      (get-response-value error :code)
                                      " message: "
                                      (get-response-value error :message)))))))

(defn get-invoke-result
  "get the invoke result , the result has two kinds:
  1. a map means a single invoke
  2. a collection means multi invoke"
  [response]
  (if (map? response)
    (get-single-invoke-result response)
    (map get-single-invoke-result response)))

(defn- remote-call
  "invoke a method with args using http"
  [endpoint-url f-read f-write  method-name args & method-args]
  (let [query (apply mk-query f-write method-name args method-args)
        response (->> query
         (http/post endpoint-url)
         :body
         (f-read))]
    (logging/debug "query:" query " response:" response)
    (get-invoke-result response)))

(defn- remote-help
  "get all the commands of the server support
   return a collection of map like
   [{:name \"conj\" :doc \"doc\" :arglist \"([x] [x y])} ... ]"
  [endpoint-url f-read]
  (-> (http/post endpoint-url)
      :body
      (f-read)))

(defn rpc-endpoint
  "Returns the endpoint to execute RPC functions."
  [& {:keys [server port on-wire]
      :or {server "localhost"
           port server/rpc-default-port on-wire "clj"}}]
  {:pre [(string? server) (< 1024 port 65535) (string? on-wire)]}
  (when-let [[f-encode f-decode] (protocol/serialization on-wire)]
    (let [url (format "http://%s:%d/%s" server port on-wire)]
      (reify RpcEndpoint
        (invoke [endpoint method-name args]
            (remote-call (str url "/invoke") f-decode f-encode method-name args))
        (multi-invoke [endpoint invokes]
            (apply remote-call
                   (str url "/invoke") f-decode f-encode invokes))
        (help [_]
              (remote-help (str url "/help") f-decode))))))

(defn invoke-rpc
  "Invoke remote func-name on endpoint with args.
  example: (invoke-rpc func1 arg1 arg2 ...)"
  [endpoint func-name & args]
  (invoke endpoint func-name args))

(defn multi-invoke-rpc
  "Invoke multi remote func-name on endpoint with args.
  args must be a collection
  example: (multi-invoke-rpc fun1 [arg1 arg2 ...] func2 [arg1 arg2 ...]])"
  [endpoint & func-args]
  (multi-invoke endpoint func-args))
