(ns clj-rpc.context
  (:require [ring.middleware.cookies :as cookies]
            [clj-rpc.user-data :as data]))

(defn wrap-context
  "accoding to the token
     (come from cookie in the http request or the token parameters)
   to get the context data , insert into the request"
  [handler fn-get-context cookie-attrs cookie-key]
  (cookies/wrap-cookies 
   (fn [request]
     (let [token (or (get-in request [:params :token])
                     (get-in request [:cookies cookie-key :value]))
           context (if (and token fn-get-context) (fn-get-context token))]
       (binding [data/*atom-token* (atom token)]
         (let [response (handler (assoc request :context context))]
           (if (and (not= token @data/*atom-token* token))
             (assoc response :cooikes {cookie-key
                                       (merge {:path "/"} cookie-attrs
                                              {:value @data/*atom-token*})})
             response))) ))))

(defn add-context
  "add context to specific command
   options is a map include several keys
   :require-context (true or false default false)
      whether this command must have context
   :params-checks
     check parameters of the method be invoked statisfy the sepcific requirements
     example :
       {0 [:username]} -->
       the first parameter must equals (get-in context [:username])"
  [command options]
  (merge command options))

(defn add-context-to-command-map
  "add context options to all the command in the command-map
   options : same as add-context"
  [command-map options]
  (into {} (map (fn [[k v]] [k (merge v options)]) command-map)))

(defn- check-authorization
  "return nil if the command don't need authrization or
   the context include the authorization information
   else return {:code :unauthorized}"
  [context command]
  (when (and (get command :require-context)
             (not (seq context)))
    {:code :unauthorized}))

(defn- check-params
  "return nil
      if the params of the command satisfy the requirement of the command
      provided specific context
   else return {:code invalid-params :message error-message}"
  [context command command-params]
  (letfn [(fn-check [[k v]]
            (if (not= (nth command-params k) (get-in context v))
              {:code :invalid-params
               :message (str "the " k "th param must be " (get-in context v ))} ))]
    (->> (:params-checks command)
         (map fn-check )
         (filter identity)
         first)))

(defn check-context
  "check whether the command and params statisfy the requirement
   if success return nil
   else return {:code error-code :message error-message}
   error-code refer to clj-rpc.rpc.clj"
  [command context params]
  (or (check-authorization context command)
      (check-params context command params)))
