(ns clj-rpc.helper
  (:require [clj-rpc.server :as server]
            [clj-rpc.client :as client]
            [clj-http.client :as http]))

;;add some convenient function to help users write integrate test
;;code.

(defn start-server
  "start test server, given a cookie-key
   and a fn-get-context function (optional)"
  [cookie-key & [fn-get-context]]
  (server/start {:join? false :port server/rpc-default-port :host "127.0.0.1"
                 :fn-get-context fn-get-context :token-cookie-key cookie-key
                 :cookie-attrs {:domain ".test.com"}}))

(defn get-http-result-cookie-value
  [result cookie-key]
  (get-in result [:cookies cookie-key :value]))

(defn- mk-client
  [on-wire & [cookie-key]]
  (let [cookie (atom nil)
        fn-post-request (fn [query url]
                  (let [result (http/post query url)
                        cv (get-http-result-cookie-value result cookie-key)]
                    (do
                      (if cv (reset! cookie cv))
                      result)))
        endpoint (client/rpc-endpoint :on-wire on-wire
                                      :fn-post-request fn-post-request)]
    (fn [method-name args & func-args]
      (apply client/invoke-rpc-with-token endpoint
             @cookie method-name args func-args))))

(def mk-clj-client (partial mk-client "clj"))

(def mk-json-client (partial mk-client "json"))

;;demo
(comment
  (def client1 mk-clj-client "cookie-key")
  (client1 "str" ["hello" "world"])
  (client1 "str" ["hello" "world"] "+" [1 2]))
