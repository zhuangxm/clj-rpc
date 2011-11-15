(ns clj-rpc.test.context
  (:use [clj-rpc.context :reload true]
        [clojure.test])
  (:require [clj-rpc.command :as command]))


(deftest check-add-context-to-command-map
  (let [command-map {"str" (command/mk-command "str" #'str)
                     "concat" (command/mk-command "concat" #'concat)}
        options {:require-context true :params-check {0 1}}
        new-map (add-context-to-command-map command-map options)
        vs (vals new-map)]
    (is (= (get-in (first vs) [:params-check]) {0 1}))
    (is (= (get-in (second vs) [:params-check]) {0 1}))))

(deftest test-check-context
  "check whether or not the command and params statifies context requirement"
  (let [cmd (->  (command/mk-command "concat" #'concat)
                 (add-context {:require-context true
                               :params-checks {0 [:p1] 1 [:p2]}}))
        cmd-not-require (command/mk-command "str" #'str)]
    (is (nil? (check-context cmd {:p1 [1 2] :p2 [3 4]}
                             [ [1 2] [3 4] [5 6]]))
        "stastify authorized and params requirement")
    (is (= :unauthorized (:code (check-context cmd nil [ [1 2] [3 4] [5 6]])))
        "do not statify authorization requirement")
    (is (= :unauthorized (:code (check-context cmd {} [ [1 2] [3 4] [5 6]])))
        "do not statify authorization requirement")
    (is (= :invalid-params (:code (check-context cmd {:p1 [1 2]}
                                                 [ [1 2] [3 4] [5 6]])))
        "do not statify params check requirement")
    (is (nil? (check-context cmd-not-require nil ["str1" "str2"]))
        "do not require context check")))
