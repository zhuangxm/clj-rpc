(ns clj-rpc.test.integrate
  (:use [clojure.test])
  (:require [clj-rpc.server :as server]
            [clj-rpc.client :as client]))


(defn around-server
  "setup and teardown test enviroment"
  [f]
  (server/start)
  (server/export-commands 'clojure.core)
  (server/export-commands 'clj-rpc.command)
  (f)
  (server/stop))

(use-fixtures :once around-server)

(defn get-help-command
  "get command named command-name"
  [command-name commands]
  (->> commands
      (filter #(= (:name %) command-name))
      first))

;.;. Woohoo! -- @zspencer
;;test can get all the commands
(deftest test-help
  (let [commands (client/help "http://localhost:8080/help")]
    (is (boolean (get-help-command "conj" commands)))
    (is (boolean (get-help-command "mk-command" commands)))))

;;test can invoke correctly.
;;include chinese characters and collection
(deftest test-invoke
  (is (= "中文测试"
         (client/call "http://localhost:8080/invoke" "str" "中文" "测试")))
  (is (= [1 2 3 4]
           (client/call "http://localhost:8080/invoke" "concat" [1 2] [3 4])))
  (is (= "j-中文j-测试"
         (client/json-call "http://localhost:8080/json/invoke" "str" "j-中文" "j-测试")))
  (is (= [0 1 2 3 4]
           (client/json-call "http://localhost:8080/json/invoke" "concat" [0] [1 2] [3 4]))))

