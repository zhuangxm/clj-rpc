(ns clj-rpc.test.command
  (:use [clj-rpc.command :reload true]
        [clojure.test]))


;"check the abstract of the command"
(deftest test-command
    (let [cmd (mk-command "mk-command" (var mk-command))]
      (is (=  (.name cmd) "mk-command"))
      (is (.doc cmd))
      (is (.arglists cmd))))

;"check a var is a function"
(deftest test-var-fn 
  (is  (not (var-fn? #'clojure.core/unquote)))
  (is  (var-fn? #'clojure.core/+)))

;"check the get-commands"
(deftest test-get-commands
  (let [fs (get-commands 'clojure.core)
        s-fs (get-commands 'clojure.core #'clojure.core/+ #'clojure.core/meta)
        cmd (get fs "meta")]
    (is (= (.name cmd) "meta") ) 
    (is (nil? (get fs "unquote")))
    (is (get s-fs "+"))
    (is (get s-fs "meta"))
    (is (not (get s-fs "-")))))

(deftest test-web-func
  (let [web+ (web-func + [:session :number])]
    (is (= 10 (web+ {:session {:number 2} :args [3 5]})))))

(deftest test-wrap-invoke
  (is (= ((wrap-invoke (web-func +)) {:params {:args "[3, 5]"}}) "8")))