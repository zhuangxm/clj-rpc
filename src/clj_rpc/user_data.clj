(ns clj-rpc.user-data
  (:require [clojure.tools.logging :as logging]
            [clj-rpc.simple-db :as db])
  (:import [java.util.concurrent ScheduledThreadPoolExecutor TimeUnit]))

;;user data format
;;data is the actual user data
;;last-visit is the last visited time (use System/currentTimeMillis) 
(defstruct session-data :data :last-visit)

(defn uuid
  "generate unique ID"
  []
  (str (java.util.UUID/randomUUID)))

(defn now
  []
  (System/currentTimeMillis))

;;define the user relate data.
;;the datas like :  {"key" (atom {:data data :last-visit last-visit}) ...}
(def user-db (db/create-db))

;;define user relate token, used for binding to per request
(def ^:dynamic *atom-token* (atom nil))

(defn get-session-data
  "internal used only, return a atom"
  [token]
  (db/get-data user-db token))

(defn get-user-data
  "the default method to get user data"
  [token]
  (:data (get-session-data token)))

(defn save-user-data!
  "sava user data , if token doesn't exist ,create a new one
   return data"
  [data]
  (do
    ;;treat @atom-token* "" as nil
    (if (not (seq @*atom-token*)) (reset! *atom-token* (uuid)) )
    (db/save-data user-db @*atom-token* (struct session-data data (now)))
    data))

(defn get-user-data!
  "get user data and modified last visited time"
  []
  (if-let [data (get-user-data @*atom-token*)]
    (save-user-data! data)))

(defn delete-user-data!
  "delete user data, only side effect ,return nil"
  []
  (do
    (db/delete-data user-db @*atom-token*)
    (reset! *atom-token* "")
    nil))

;;clean the user data that has been expired.

(defn clean-timeout-data
  [data now timeout]
  (into {}
        (remove (fn [[k {last-visit :last-visit}]]
                  (> (- now last-visit) timeout))
                data)))


(defn clean-timeout!
  "clean all expired user data
   now : the time of now , should be System/currentTimeMiles
   time-out : time out peroid, use millsecond as unit."
  [now time-out]
  (let [ks (keys @user-db)]
    (doseq [k ks]
      (when-let [last-visit (get (get-session-data k) :last-visit 0)]
        (when (> (- now last-visit) time-out)
          (db/delete-data user-db k))))))

(defn with-log-clean-timeout!
  "clean all expired user data
   now : the time of now , should be System/currentTimeMiles
   time-out : time out peroid, use millsecond as unit.
   TODO : It's ugly here, to make the code better. "
  [now timeout]
  (logging/debug "begin clean timeout user data count : "
                 (count @user-db))
  (clean-timeout! now timeout)
  (logging/debug "after clean timeout user data count : "
                 (count @user-db)))

(defn periodical-clean-data!
  "clean all expired user data every interval millsecond"
  [interval timeout]
  (let [scheduler (ScheduledThreadPoolExecutor. 1)
        todo (fn [] (with-log-clean-timeout! (now) timeout))]
    (.scheduleAtFixedRate scheduler todo interval interval TimeUnit/MILLISECONDS)))
