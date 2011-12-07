(ns clj-rpc.test.integrate
  (:use [clojure.test]
        [midje.sweet])
  (:require [clj-rpc.server :as server]
            [clj-rpc.client :as client]
            [clj-rpc.user-data :as store]
            [clj-http.client :as http]
            [cheshire.core :as json]))

(defn fn-with-context-check
  "test function to export"
  [username p]
  (str username p))

(defn get-context [token]
  (get {"111" {:username "user111"} "222" {:username "user222"}}
       token))

(defn fn-save-data
  []
  (store/save-user-data! "333"))

(defn fn-get-data
  []
  (store/get-user-data!))

(defn fn-delete-data
  []
  (store/delete-user-data!))


(defn setup []
  (server/start {:join? false :port server/rpc-default-port :host "127.0.0.1"
                 :fn-get-context get-context :token-cookie-key "hjd-session"
                 :cookie-attrs {:domain ".test.com"}})
  (server/export-commands 'clojure.core nil)
  (server/export-commands "clj-rpc.command" ['mk-command "get-commands"])
  (server/export-commands "clj-rpc.test.integrate" ['fn-with-context-check]
                          {:require-context true :params-check {0 [:username]} })
  (server/export-commands 'clj-rpc.test.integrate
                          ['fn-save-data 'fn-get-data 'fn-delete-data]))

(defn tear-down []
  (server/stop))

(defn around-server
  "setup and teardown test enviroment"
  [f]
  (setup)
  (f)
  (tear-down))

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

;;using midje to test cookie support
(def default-url (str "http://localhost:"
                                 server/rpc-default-port "/json/invoke"))

(defn mk-data-query
  [method-name]
  (client/mk-query json/generate-string
                   {:method method-name :params nil}))

(defn decode-result [result]
  (json/parse-string (get-in result [:body])))

(defn invoke-data-query
  [query & [cookie]]
  (let [query (if cookie (assoc query :cookies {"hjd-session" {:value cookie}})
                  query)] 
    (http/post default-url query)))

(defn invoke-and-get-result
  [query cookie]
  (decode-result (invoke-data-query query cookie)) )

;;midje test main body
(against-background [(before :contents (setup))
                     (after :contents (tear-down))]
  (facts "test cookie support, can set-cookie, can get data using cookie"
    (let [query-save (mk-data-query "fn-save-data")
          query-get (mk-data-query "fn-get-data")
          query-delete (mk-data-query "fn-delete-data")
          result-save (invoke-data-query query-save)
          cookie-m (get-in result-save [:cookies "hjd-session"])
          cookie (get cookie-m :value)
          _ (prn cookie-m)]
      ;;can set cookie
      cookie-m => (contains {:domain ".test.com" :path "/" :value truthy})
      ;;can get data by cookie
      (invoke-and-get-result query-get cookie) => (contains {"result"  "333"})
      ;;can not get data using incorrect cookie
      (invoke-and-get-result query-get "other") => (contains {"result" nil})
      ;;different cookie doesn't disturbe each other
      (invoke-and-get-result query-get cookie) => (contains {"result"  "333"})
      ;;can delete user data
      (invoke-and-get-result query-delete cookie) => (contains {"result" nil})
      ;;can not get data when the user data has been deleted.
      (invoke-and-get-result query-get cookie) => (contains {"result" nil}))))
