(ns clj-rpc.user-data
  (:require [clojure.tools.logging :as logging]))

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
(def atom-user-datas (atom {}))

;;define user relate token, used for binding to per request
(def ^:dynamic *atom-token* (atom nil))

(defn get-session-data
  "internal used only"
  [token]
  (get @atom-user-datas token))

(defn get-user-data
  "the default method to get user data"
  [token]
  (get-in @atom-user-datas [token :data]))

(defn save-user-data!
  "sava user data , if token doesn't exist ,create a new one
   return data"
  [data]
  (do
    ;;treat @atom-token* "" as nil
    (if (not (seq @*atom-token*)) (reset! *atom-token* (uuid)) )
    (swap! atom-user-datas assoc @*atom-token*
           (struct session-data data (now)) )
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
    (swap! atom-user-datas dissoc @*atom-token*)
    (reset! *atom-token* "")
    nil))

;;clean the user data that has been expired.

(defn clean-timeout!
  "clean all expired user data
   now : the time of now , should be System/currentTimeMiles
   time-out : time out peroid, use millsecond as unit."
  [now time-out]
  (let [ks (keys @atom-user-datas)]
    (doseq [k ks]
      (when-let [last-visit (get-in @atom-user-datas [k :last-visit])]
        (when (> (- now last-visit) time-out)
          (swap! atom-user-datas dissoc k))))))

(defn time-clean-timeout!
  "clean all expired user data every interval millsecond"
  [interval time-out]
  (.start  (Thread. (fn []
                      (loop []
                        (do 
                          (Thread/sleep interval)
                          (logging/debug "begin clean timeout user data count : "
                                         (count @atom-user-datas))
                          (clean-timeout! (now) time-out)
                          (logging/debug "after clean timeout user data count : "
                                         (count @atom-user-datas))
                          (recur)))) )))
