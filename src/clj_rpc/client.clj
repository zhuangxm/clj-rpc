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
  (help [endpoint]
    "Returns the list of the functions that endpoint support."))

(def ^:private content-type "charset=UTF-8")

(defn- mk-query
  "make query object to send to endpoint."
  [f-encode method-name args]
  (let [body (f-encode {:method method-name :params args}) ]
    {:body body :content-type content-type}))

(defn get-response-value
  "get response key value ,either keywork or string"
  [m key]
  (or (get m key) (get m (name key))))

(defn- remote-call
  "invoke a method with args using http"
  [endpoint-url f-read f-write  method-name args]
  (let [query (mk-query f-write method-name args)
        response (->> query
         (http/post endpoint-url)
         :body
         (f-read))
        error (get-response-value response :error)]
    (logging/debug "query:" query " response:" response)
    (if (not error)
      (get-response-value response :result)
      (throw (RuntimeException. (str  "error cdoe : "
                                      (get-response-value error :code)
                                      " message: "
                                      (get-response-value error :message)))))))

(defn- remote-help
  "get all the commands of the server support
   return a collection of map like
   [{:name \"conj\" :doc \"doc\" :arglist \"([x] [x y])} ... ]"
  [endpoint-url f-read]
  (-> (http/get endpoint-url)
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
        (help [_]
          (remote-help (str url "/help") f-decode))))))

(defn invoke-rpc
  "Invoke remote func-name on endpoint with args."
  [endpoint func-name & args]
  (invoke endpoint func-name args))
