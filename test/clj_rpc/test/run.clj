(ns clj-rpc.test.run
  (:use [midje.sweet]
        [clj-rpc.helper])
  (:require [clj-rpc.user-data :as store]
            [clj-rpc.server :as server]
            [clj-rpc.client :as client]))

(defn long-run []
  (Thread/sleep 6000)
  3)

(defn setup
  []
  (start-server "hello")
  (server/export-commands 'clj-rpc.test.run
                          ["long-run"]))

(defn tear-down
  []
  (server/stop))

(defn multi-run []
  (dotimes [i 10]
    (let [client (mk-clj-client "hello")]
      (.start
       (Thread. (fn [] (do (println "thread"  i)
                          (client "long-run" nil)
                         (println "thread" i "finished")) ))))))

(against-background
  [(before :contents (setup)) (after :contents (tear-down))]

  (fact "test conn timeout"
        (let [endp (client/rpc-endpoint :server "127.0.0.3" :conn-timeout 5000)]
          (client/invoke-rpc endp "long-run" []) => (throws org.apache.http.conn.ConnectTimeoutException)))

  (fact "test socket timeout"
        (let [endp (client/rpc-endpoint :socket-timeout 5000)]
          (client/invoke-rpc endp "long-run" []) => (throws java.net.SocketTimeoutException))

        (let [endp (client/rpc-endpoint :socket-timeout 7000)]
          (client/invoke-rpc endp "long-run" []) => 3)))
