(ns clj-rpc.test.helper
  (:use [midje.sweet]
        [clj-rpc.rpc]
        [clj-rpc.error]))
(fact
  (mk-error :parse-error 0) => (just {:id 0 :error {:code -32700 :message "parse protocol error" :data ""}})
  (mk-error 12 0 "msg") => (just {:id 0 :error {:code 12 :message "msg" :data ""}})
  (mk-error 12 0 "msg" :data) => (just {:id 0 :error {:code 12 :message "msg" :data :data}})
  (execute-method raise-error {:params [27 "error"]})
  => (just {:id nil :error {:code 27 :message "error" :data ""}})
  (execute-method raise-error {:params [27 "error" :data]})
  => (just {:id nil :error {:code 27 :message "error" :data :data}}))
