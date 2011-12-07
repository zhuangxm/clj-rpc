(ns clj-rpc.context
  (:require [ring.middleware.cookies :as cookies]
            [clj-rpc.user-data :as data]))


;;the options availbe to context
(def context-options [:require-context :params-check :params-inject])

(defn wrap-client-ip
  "let :remote-addr represents the real client ip even though
   the client connects through a proxy server"
  [handler]
  (fn [request]
    (let [ip-from-proxy (get-in request [:headers "X-Forwarded-For"])
          request (if ip-from-proxy
                    (assoc request :remote-addr ip-from-proxy)
                    request)]
      (handler request))))

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
             (assoc response :cookies {cookie-key
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
       the first parameter must equals (get-in context [:username])
   :params-inject
      inject parameters from request into the function
      example :
       [ [:remote-addr] [:server-name] ] -->
       client-ip and server-name as the first and second parameter of the function
       if client invoke (fun param1 param2) then the acutally invoke will be like
       (fun client-ip server-name param1 param2)"
  [command options]
  {:pre [ (every? #(% (set context-options)) (keys options))]}
  (merge command options))

(defn add-context-to-command-map
  "add context options to all the command in the command-map
   options : same as add-context"
  [command-map options]
  (into {} (map (fn [[k v]] [k (add-context v options)]) command-map)))

(defn error-method-request?
  "return true if method-reqeust error
  else false"
  [method-request]
  (boolean (or (:code method-request)
               (:message method-request))))

(defmulti pre-handle-method-request
  "pre handle method request
   return new method-request if successful
   else return error like {:code error-code :message error-message}"
  (fn [option-key option request method-request]
    (if (not (error-method-request? method-request))
      option-key
      :default)))

;;pre handle :require-context option
;;return method-request if the command don't need authrization or
;;the context include the authorization information
;;else return {:code :unauthorized}
(defmethod pre-handle-method-request :require-context
  [_ option request method-request]
  (let [context (get request :context)]
    (if (and option
             (not (seq context)))
      {:code :unauthorized}
      method-request)))

;;pre handle :params-check
;;return method-request if the params of the command satisfy
;;the requirement of the command provided specific context
;;else return {:code invalid-params :message error-message}
(defmethod pre-handle-method-request :params-check
  [_ option request method-request]
  (let [context (get request :context)
        params (:params method-request)
        fn-check
        (fn [[k v]]
            (if (not= (nth params k) (get-in context v))
              {:code :invalid-params
               :message (str "the " k "th param must be " (get-in context v))}))]
    (or (->> option
             (map fn-check )
             (filter identity)
             first)
        method-request)))

;;pre handle :params-inject
;;inject the params from request needed by the command
;;into the actualparams
;;return the new method-request
(defmethod pre-handle-method-request :params-inject
  [_ option request method-request]
  (if option
    (update-in method-request [:params]
               #(concat (map (partial get-in request) option) %))
    method-request))

;;default return origin method-request
(defmethod pre-handle-method-request :default
  [_ option request method-request]
  method-request)

(defn check-context
  "handle options of the command by order
   and return  new method-request or error message"
  [cmd request method-request]
  (reduce (fn [m-r k] (pre-handle-method-request k (get cmd k) request m-r))
          method-request
          context-options))
