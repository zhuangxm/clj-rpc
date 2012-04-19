(ns clj-rpc.test.multi-server
  (:use [midje.sweet])
  (:require [clj-rpc.server :as server]
            [clj-rpc.client :as client]))

(defn fn1 [] "fn1")

(defn fn2 [] "fn2")

(defn setup []
  (server/export-commands 'clj-rpc.test.multi-server ['fn1])
  (server/start {:join? false :port 8990})
  (let [new-commands (atom {})]
    (server/with-commands new-commands
      (server/export-commands 'clj-rpc.test.multi-server ['fn2]))
    (def another-server (server/start {:join? false :commands new-commands
                                       :port 8991}))))

(defn tear-down  []
  (server/stop another-server)
  (server/stop))

(against-background [(before :contents (setup))
                     (after :contents (tear-down))]
  (facts "test multi server"
    (let [endp1 (client/rpc-endpoint :port 8990)
          endp2 (client/rpc-endpoint :port 8991)]
      (client/invoke-rpc endp1 "fn1" []) => "fn1"
      (client/invoke-rpc endp2 "fn2" []) => "fn2"
      (client/invoke-rpc endp1 "fn2" []) => (throws RuntimeException)
      (client/invoke-rpc endp2 "fn1" []) => (throws RuntimeException) )))
