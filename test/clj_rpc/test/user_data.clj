(ns clj-rpc.test.user-data
  (:use [clj-rpc.user-data]
        [midje.sweet]))

(facts "test user-data"
  (reset! atom-user-datas nil) => nil
  (get-user-data!) => nil
  (save-user-data! 30) => 30
  (get-user-data!) => 30
  (clean-timeout! (now) 1000) => nil
  (count @atom-user-datas) => 1
  (Thread/sleep 1100)
  (binding [*atom-token* (atom "333")]
    (save-user-data! 100) => 100)
  (count @atom-user-datas) => 2
  (clean-timeout! (now) 1000) => nil
  @atom-user-datas => map?
  (count @atom-user-datas) => 1)
