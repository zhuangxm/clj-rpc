(ns clj-rpc.test.adjust-response
  (:use [midje.sweet]
        [clj-rpc.helper])
  (:require [clj-rpc.context :as context]
            [clj-rpc.user-data :as store]
            [clj-rpc.server :as server]))

;;test can define new mutlimethod function of command options
(defn succ-add
  "连续+"
  [x]
  (let [v (or (store/get-user-data!) 0)
        r (+ x v)]
    (store/save-user-data! r)))

;;测试使用的cookie key name
(def cookie-key-name "test-cookie-name")

;;this option do nothing with method request
(defmethod context/render-method-request :add
  [option-key option-value request method-request]
  method-request)

;;this option add value to result and save it to the session
(defmethod context/render-response :add
  [option-key option-value request response]
  (let [result (:result response)
        new-result (+ result option-value)]
    (do (store/save-user-data! new-result)
        (assoc response :result new-result))))

(defn setup
  []
  (start-server cookie-key-name)
  ;;export command with new define option
  (server/export-commands 'clj-rpc.test.adjust-response ["succ-add"] [[:add 3]] ))

(against-background [(before :contents (setup))
                     (after :contents (server/stop))]
  (facts "test clients are independent"
    (let [client1 (mk-clj-client cookie-key-name)
          client2 (mk-json-client cookie-key-name)]
      (client1 "succ-add" [20]) => 23
      (client2 "succ-add" [2]) => 5
      (client1 "succ-add" [40]) => 66
      (client2 "succ-add" [6]) => 14)))

