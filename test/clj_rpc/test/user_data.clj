(ns clj-rpc.test.user-data
  (:use [clj-rpc.user-data]
        [clj-rpc.simple-db :as db]
        [midje.sweet]))

(facts "test user-data"
  (reset! user-db nil) => nil
  (get-user-data!) => nil
  (save-user-data! 30) => 30
  (get-user-data!) => 30
  (clean-timeout! (now) 1000) => nil
  (db/data-count user-db) => 1
  (Thread/sleep 1100)
  (binding [*atom-token* (atom "333")]
    (save-user-data! 100) => 100)
  (db/data-count user-db) => 2
  (clean-timeout! (now) 1000) => nil
  @user-db => map?
  (db/data-count user-db) => 1)
