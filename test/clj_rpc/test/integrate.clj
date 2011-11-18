(ns clj-rpc.test.integrate
  (:use [clojure.test])
  (:require [clj-rpc.server :as server]
            [clj-rpc.client :as client]))

;;TODO test tokne in the cookie.

(defn fn-with-context-check
  "test function to export"
  [username p]
  (str username p))

(defn get-context [token]
  (get {"111" {:username "user111"} "222" {:username "user222"}}
       token))

(defn around-server
  "setup and teardown test enviroment"
  [f]
  (server/start {:join? false :port server/rpc-default-port :host "127.0.0.1"
                 :fn-get-context get-context :token-cookie-key "hjd-session"})
  (server/export-commands 'clojure.core nil)
  (server/export-commands "clj-rpc.command" ['mk-command "get-commands"])
  (server/export-commands "clj-rpc.test.integrate" ['fn-with-context-check]
                          {:require-context true :params-checks {0 [:username]} })
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
           (client/invoke-rpc endp "str" ["中文" "测试"])))
    (is (= [1 2 3 4]
           (client/invoke-rpc endp "concat" [ [1 2] [3 4] ])))))


(deftest test-multi-invoke
  (doseq [endp (map #(client/rpc-endpoint :on-wire %) ["clj" "json"])]
    (is (= ["中文测试" [1 2 3 4]]
             (client/invoke-rpc endp "str" ["中文" "测试"]
                                  "concat" [[1 2] [3 4]])))))

(deftest test-invoke-with-token
  (doseq [endp (map #(client/rpc-endpoint :on-wire %) ["clj" "json"])]
    (is (= "user111---"
           (client/invoke-rpc-with-token endp "111" "fn-with-context-check"
             ["user111" "---"]))
        "execute inovke-rpc-with-token correctly")
    (is (= ["user111---" "user111???"]
           (client/invoke-rpc-with-token endp "111" "fn-with-context-check"
             ["user111" "---"] "fn-with-context-check" ["user111" "???"]))
        "execute multi-invoker-rpc-with-token")
    (is (thrown? RuntimeException
                 (client/invoke-rpc-with-token endp "111" "fn-with-context-check"
                   ["user222" "---"]) )
        "params check failure")
    (is (thrown? RuntimeException
                 (client/invoke-rpc-with-token endp "333" "fn-with-context-check"
                   ["user222" "---"]) )
        "get context failure")))
