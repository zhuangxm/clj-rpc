(ns clj-rpc.test.error
  (:use [midje.sweet]
        [clj-rpc.error]))

(fact
  (try (raise-error 20 "raise error" {:username "skz"})
       (catch clojure.lang.ExceptionInfo re
         (do
           (:code (ex-data re)) => 20
           (:data (ex-data re)) => {:username "skz"}
           (.getMessage re) => "raise error"))))
