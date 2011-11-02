(ns clj-rpc.test.integrate
  (:use [clojure.test])
  (:require [clj-rpc.server :as server]
            [clj-rpc.client :as client]))

(defn around-server
  "setup and teardown test enviroment"
  [f]
  (server/start)
  (f)
  (server/stop))

(use-fixtures :once around-server)

(deftest test-invoke
  (server/export-func str "str")
  (server/export-func concat "concat")
  (doseq [endp (map #(client/rpc-endpoint :on-wire %) ["clj" "json"])]
    (is (= "中文测试"
           (client/invoke-rpc endp "str" "中文" "测试")))
    (is (= [1 2 3 4]
           (client/invoke-rpc endp "concat" [1 2] [3 4])))))
