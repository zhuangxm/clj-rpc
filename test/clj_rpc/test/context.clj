(ns clj-rpc.test.context
  (:use [clj-rpc.context :reload true]
        [clojure.test]
        [midje.sweet])
  (:require [clj-rpc.command :as command]))

(deftest check-add-context-to-command-map
  (let [command-map {"str" (command/mk-command "str" #'str)
                     "concat" (command/mk-command "concat" #'concat)}
        options [ [:require-context true] [:params-check {0 1}]]
        new-map (add-context-to-command-map command-map options)
        vs (vals new-map)]
    (is (= (get (first vs) :options) options))
    (is (= (get (second vs) :options) options))))

(facts 
  "check whether or not the command and params statifies context requirement"
  (let [cmd (->  (command/mk-command "concat" #'concat)
                 (add-context [ [:require-context true]
                                [:params-check {0 [:p1] 1 [:p2]}]]))
        cmd-not-require (command/mk-command "str" #'str)
        method-request {:method "method" :params [ [1 2] [3 4] [5 6]]}]
    ;;stastify authorized and params requirement
    (adjust-method-request cmd {:context {:p1 [1 2] :p2 [3 4]}}
                   method-request) => method-request

    ;;do not statify authorization requirement
    (adjust-method-request cmd nil method-request) => (contains {:error {:code :unauthorized}})
    (adjust-method-request cmd {} method-request) => (contains {:error {:code :unauthorized}})

    ;;do not statify params check requirement
    (get-in (adjust-method-request cmd {:context {:p1 [1 2] }} method-request)
            [:error :code]) => :invalid-params

    ;;do not require context check
    (adjust-method-request cmd-not-require nil {:method "method" :params ["str1" "str2"]})
    => {:method "method" :params ["str1" "str2"]}))

;.;. The work itself praises the master. -- CPE Bach
(facts "test inject parasm"
  (let [cmd (->  (command/mk-command "str" #'str)
                 (add-context {:params-inject [ [:remote-addr] [:inject]]})) ]
    (adjust-method-request cmd {:remote-addr "192.168.1.1" :inject "inject-param2"}
                                     {:method "method" :params ["p1" "p2"]})
    => {:method "method" :params ["192.168.1.1" "inject-param2" "p1" "p2"]}))
