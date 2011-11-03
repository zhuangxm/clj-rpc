(ns clj-rpc.test.command
  (:use [clj-rpc.command :reload true]
        [clojure.test]))

;"check a var is a function"
(deftest test-var-fn 
  (is  (not (var-fn? #'clojure.core/unquote)))
  (is  (var-fn? #'clojure.core/+)))

(deftest test-web-func
  (let [web+ (web-func + [:session :number])]
    (is (= 10 (web+ {:session {:number 2} :args [3 5]})))))

(deftest test-wrap-invoke
  (is (= ((wrap-invoke (web-func +)) {:params {:args "[3, 5]"}}) "8")))