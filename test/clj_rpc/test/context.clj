(ns clj-rpc.test.context
  (:use [clj-rpc.context :reload true]
        [clojure.test]
        [midje.sweet])
  (:require [clj-rpc.command :as command]))

(deftest check-add-context-to-command-map
  (let [command-map {"str" (command/mk-command "str" #'str)
                     "concat" (command/mk-command "concat" #'concat)}
        options {:require-context true :params-check {0 1}}
        new-map (add-context-to-command-map command-map options)
        vs (vals new-map)]
    (is (= (get-in (first vs) [:params-check]) {0 1}))
    (is (= (get-in (second vs) [:params-check]) {0 1}))))

(facts 
  "check whether or not the command and params statifies context requirement"
  (let [cmd (->  (command/mk-command "concat" #'concat)
                 (add-context {:require-context true
                               :params-check {0 [:p1] 1 [:p2]}}))
        cmd-not-require (command/mk-command "str" #'str)
        method-request {:method "method" :params [ [1 2] [3 4] [5 6]]}]
    ;;stastify authorized and params requirement
    (check-context cmd {:context {:p1 [1 2] :p2 [3 4]}}
                   method-request) => method-request

    ;;do not statify authorization requirement
    (check-context cmd nil method-request) => (contains {:code :unauthorized})
    (check-context cmd {} method-request) => (contains {:code :unauthorized})

    ;;do not statify params check requirement
    (check-context cmd {:context {:p1 [1 2] }} method-request) => (contains {:code :invalid-params})

    ;;do not require context check
    (check-context cmd-not-require nil {:method "method" :params ["str1" "str2"]})
    => {:method "method" :params ["str1" "str2"]}))
