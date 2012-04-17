(ns clj-rpc.test.helper
  (:use [midje.sweet]
        [clj-rpc.exception]))

(def e (clj_rpc.CodeException. 27 "message"))
(fact
  (instance? Exception e) => true
  (.getCode e) => 27
  (.getMessage e) => "message"

  (try (raise-error 20 "raise error")
       (catch clj_rpc.CodeException re
         (do
           (.getCode re) => 20
           (.getMessage re) => "raise error"))))
