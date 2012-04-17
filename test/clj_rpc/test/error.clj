(ns clj-rpc.test.error
  (:use [midje.sweet]
        [clj-rpc.error]))

(def e (clj_rpc.Exception. 27 "message"))
(fact
  (instance? Exception e) => true
  (.getCode e) => 27
  (.getMessage e) => "message"

  (try (raise-error 20 "raise error")
       (catch clj_rpc.Exception re
         (do
           (.getCode re) => 20
           (.getMessage re) => "raise error"))))
