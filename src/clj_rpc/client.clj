(ns clj-rpc.client
  (:require [clj-http.client :as http]
            [clj-http.util :as util]
            [cheshire.core :as json]))

;;TODO to ugly to supplement json protocol support
;;TODO should make it beautiful after refactoring.
;;TODO should hide the internal url tag like /json/help or /help
;;TODO should use serveraddress and port as url.

;;only make the json work.

(defn- call-internal
  "invoke a method with args using http
   return http response. make it easy to debug"
  [endpoint-url f-write method-name & args]
  (let [query (str "method=" (util/url-encode method-name)
                   "&args=" (util/url-encode (f-write args)))]
    (http/post endpoint-url
               {:body query
                :content-type
                "application/x-www-form-urlencoded; charset=UTF-8"})))

(defn common-call
  "invoke a method with args using http"
  [endpoint-url f-read f-write  method-name & args]
  (let [http-result (apply call-internal endpoint-url f-write method-name args) ]
    (-> http-result
        :body
        (f-read))))

(defn call
  "use clojure read-string pr-str as protocol"
  [endpoint-url method-name & args]
  (apply common-call endpoint-url read-string pr-str method-name args))

(defn json-call
  "use json protocol"
  [endpoint-url method-name & args]
  (apply common-call endpoint-url json/parse-string
         json/generate-string method-name args))

(defn common-help
  "get all the commands of the server support
   return a collection of map like
   [{:name \"conj\" :doc \"doc\" :arglist \"([x] [x y])} ... ]"
  [endpoint-url f-read]
  (-> (http/get endpoint-url )
      :body
      (f-read)))

(defn help
  "help use clojure protocol"
  [endpoint-url]
  (common-help endpoint-url read-string))

(defn json-help
  [endpoint-url]
  (common-help endpoint-url json/parse-string))
