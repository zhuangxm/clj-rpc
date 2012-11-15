(ns clj-rpc.test.helper
  (:use [midje.sweet]
        [clj-rpc.helper])
  (:require [clj-rpc.user-data :as store]
            [clj-rpc.server :as server]))

(defn succ-add
  "连续+"
  [x]
  (let [v (or (store/get-user-data!) 0)
        r (+ x v)]
    (store/save-user-data! r)))

(defn error-func
  []
  (/ 1 0))

(defn log-func
  [s]
  (apply str (reverse s)))

(defn logout
  []
  (store/delete-user-data!))

;;测试使用的cookie key name
(def cookie-key-name "test-cookie-name")

(defn setup
  []
  (start-server cookie-key-name)
  (server/export-commands 'clj-rpc.test.helper
                          ["succ-add" "logout" "error-func"])
  (server/export-commands 'clj-rpc.test.helper
                          ["log-func"] [ [:log :warn] ]))

;.;. For every disciplined effort, there is a multiple reward. -- Rohn
(against-background [(before :contents (setup))
                     (after :contents (server/stop))]
  (facts "test clients are independent"
    (let [client1 (mk-clj-client cookie-key-name)
          client2 (mk-json-client cookie-key-name)]
      (client1 "succ-add" [30]) => 30
      (client2 "succ-add" [3]) => 3
      (client1 "succ-add" [50]) => 80
      (client2 "succ-add" [5]) => 8
      (client1 "succ-add" [50]) => 130
      (client2 "succ-add" [5]) => 13
      (client1 "logout" nil) => nil
      (client2 "succ-add" [5]) => 18
      (client1 "succ-add" [50]) => 50
      (client2 "logout" nil) => nil
      (client1 "succ-add" [50]) => 100
      (client2 "succ-add" [5]) => 5
      (client2 "log-func" ["hello"]) => "olleh"
      (client2 "error-func" []) => (throws RuntimeException))))

