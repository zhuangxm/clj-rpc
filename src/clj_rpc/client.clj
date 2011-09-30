(ns clj-rpc.client
  (:require [clj-http.client :as http]
            [clj-http.util :as util]))

(defn call
  "invoke a method with args using http"
  [endpoint-url method-name & args]
  (let [query (str "method=" (util/url-encode method-name)
                   "&args=" (util/url-encode (pr-str args)))]
    (->
     (http/post endpoint-url
                {:body query
                 :content-type "application/x-www-form-urlencoded; charset=UTF-8"})
     :body
     (read-string))))


(defn help
  "get all the commands of the server support"
  [endpoint-url]
  (-> (http/get endpoint-url )
      :body
      (read-string)))
