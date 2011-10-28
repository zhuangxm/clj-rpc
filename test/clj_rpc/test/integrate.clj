(ns clj-rpc.test.integrate
  (:use [clojure.test])
  (:require [clj-rpc.server :as server]
            [clj-rpc.client :as client]))

(defn around-server
  "setup and teardown test enviroment"
  [f]
  (server/start)
  (server/export-commands 'clojure.core)
  (server/export-commands "clj-rpc.command" 'mk-command "get-commands")
  (f)
  (server/stop))

(use-fixtures :once around-server)

(defn get-help-command
  "get command named command-name"
  [command-name commands]
  (->> commands
      (filter #(= (:name %) command-name))
      first))

(deftest test-help
  (let [commands (client/help (client/rpc-endpoint))]
    (is (get-help-command "conj" commands))
    (is (get-help-command "mk-command" commands))
    (is (get-help-command "get-commands" commands))
    (is (not (get-help-command "execute-method" commands)))))

;;test can invoke correctly.
;;include chinese characters and collection
(deftest test-invoke
  (doseq [endp (map #(client/rpc-endpoint :on-wire %) ["clj" "json"])]
    (is (= "中文测试"
           (client/invoke-rpc endp "str" "中文" "测试")))
    (is (= [1 2 3 4]
           (client/invoke-rpc endp "concat" [1 2] [3 4])))))

