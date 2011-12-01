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


;;define a method request structure
(defstruct str-method-request :method :params)

(defprotocol RpcEndpoint
  (invoke [endpoint method-request token]
    "Invoke functions with name func-name with arguments args on endpoint,
    parameter method-request can be a method-request  structure like
         {:methd-name \"str\" :params [\"hello\" \"world\"]}
         or a collection of method-request.")
  (help [endpoint]
    "Returns the list of the functions that endpoint support."))

(def ^:private content-type "charset=UTF-8")

(defn mk-query
  "make query object to send to endpoint."
  [f-encode method-request]
  (let [body (f-encode method-request) ]
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

(defn ^:dynamic post-request
  "supply a method that can be dynamic binding , convenient to test
   TODO: looks ugly, any suggestion?"
  [query url]
  (http/post query url))

(defn- remote-call
  "invoke a method with args using http"
  [endpoint-url f-read f-write  invoke-request]
  (let [query (mk-query f-write invoke-request)
        response (->> query
         (post-request endpoint-url)
         :body
         (f-read))]
    (logging/debug "url:" endpoint-url " query:" query " response:" response)
    (get-invoke-result response)))

(defn- remote-help
  "get all the commands of the server support
   return a collection of map like
   [{:name \"conj\" :doc \"doc\" :arglist \"([x] [x y])} ... ]"
  [endpoint-url f-read]
  (-> (http/post endpoint-url)
      :body
      (f-read)))

(defn- invoke-url [url token]
  (let [url (str url "/invoke")]
    (if token (str url "?token=" token) url)))

(defn rpc-endpoint
  "Returns the endpoint to execute RPC functions."
  [& {:keys [server port on-wire]
      :or {server "localhost"
           port server/rpc-default-port on-wire "clj"}}]
  {:pre [(string? server) (< 1024 port 65535) (string? on-wire)]}
  (when-let [[f-encode f-decode] (protocol/serialization on-wire)]
    (let [url (format "http://%s:%d/%s" server port on-wire)]
      (reify RpcEndpoint
        (invoke [endpoint token method-request]
          (remote-call (invoke-url url token)
                       f-decode f-encode method-request))
        (help [_]
              (remote-help (str url "/help") f-decode))))))

(defn invoke-rpc-with-token
  "Invoke remote func-name on endpoint with args.
  example: (invoke-rpc-with-token token func1 [arg1 arg2 ...] func2 [arg ...])
  if only one request return only one result,
  otherwise return collection of result"
  [endpoint token method-name args & func-args]
  (let [method-request 
        (if (seq func-args)
          (map #(apply struct str-method-request %)
               (partition 2 2 (concat [method-name args] func-args)))
          (struct str-method-request method-name args))]
    (invoke endpoint token method-request)))

(defn invoke-rpc
  "Invoke one or more remote func-name on endpoint with args.
  example: (invoke-rpc fun1 [arg1 arg2 ...] func2 [arg1 arg2 ...]])
  if only one request return only one result,
  otherwise return collection of result"
  [endpoint method-name args & func-args]
  (apply invoke-rpc-with-token endpoint nil method-name args func-args))
