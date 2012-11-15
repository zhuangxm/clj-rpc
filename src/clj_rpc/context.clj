(ns clj-rpc.context
  (:require [ring.middleware.cookies :as cookies]
            [clj-rpc.user-data :as data]
            [clojure.tools.logging :as logging]))

(defn get-proxy-ip
  [request]
  (or (get-in request [:headers "x-forwarded-for"])
      (get-in request [:headers "x-real-ip"])))

(defn wrap-client-ip
  "let :remote-addr represents the real client ip even though
   the client connects through a proxy server"
  [handler]
  (fn [request]
    (let [ip-from-proxy (get-proxy-ip request)
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
           (if (not= token @data/*atom-token*)
             (assoc response :cookies {cookie-key
                                       (merge {:path "/" :http-only true}
                                              cookie-attrs
                                              {:value @data/*atom-token*})})
             response))) ))))

(defn add-context
  "add context to specific command
   options is a collection include several keys
   like [ [:requre-context true] [:params-check ...] [... ...] ]
   
   the meaning of option:
   :require-context (true or false default false)
      whether this command must have context
   :params-check
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
  (assoc command :options options))

(defn add-context-to-command-map
  "add context options to all the command in the command-map
   options : same as add-context"
  [command-map options]
  (into {} (map (fn [[k v]] [k (add-context v options)]) command-map)))

(defn error-method-request?
  "return true if method-reqeust error
  else false"
  [method-request]
  (boolean (:error method-request)))

(defmulti render-method-request
  "adjust method request by option
   return new method-request"
  (fn [option-key option-value request method-request]
    option-key))

;;render method-request :require-context option
;;return method-request with error or original method-request
(defmethod render-method-request :require-context
  [_ option-value request method-request]
  (if (and option-value
             (not (seq (get request :context) )))
    (assoc method-request :error {:code :unauthorized})
    method-request))

;;render method-request :params-check option
;;return method-request with params error or original method-request
(defmethod render-method-request :params-check
  [_ option-value request method-request]
  (let [{context :context} request
        {params :params} method-request
        fn-check
        (fn [[k v]]
            (if (not= (nth params k) (get-in context v))
              {:code :invalid-params
               :message (str "the " k "th param must be "
                             (get-in context v))}))]
    (if-let [error (->> option-value
                (map fn-check )
                (filter identity)
                first)]
      (assoc method-request :error error)
      method-request)))

;;render method-request :params-inject option
;;inject the params from request needed by the command
;;into the actual params
;;return the new method-request
(defmethod render-method-request :params-inject
  [_ option-value request method-request]
  (if option-value
    (update-in method-request [:params]
               #(concat (map (partial get-in request) option-value) %))
    method-request))

;;render :log option
;;just return the original method-request to make sure we need this option
(defmethod render-method-request :log
  [_ option-value request method-request]
  method-request)

;;default throw RuntimeException
(defmethod render-method-request :default
  [option-key option-value request method-request]
  (throw (RuntimeException. (str "Unknown command option: " option-key
                                 "method-request: " method-request )  )) )

(defn adjust-method-request
  "return new method-request (possible with error message)"
  [cmd request method-request]
  (loop [options (:options cmd)
         m-r method-request]
    (let [[option-key option-value] (first options)]
      (if (or (nil? option-key) (error-method-request? m-r))
        m-r
        (recur (rest options)
               (render-method-request option-key option-value request m-r))))))

(defmulti render-response
  "adjust method response or do some side-effects by option
   return new response"
  (fn [option-key option-value request method-response]
    option-key))

;;default return response
(defmethod render-response :default
  [option-key option-value request response]
  response)

;;log client-ip method-request and response
(defmethod render-response :log
  [option-key option-value request response]
  (logging/log option-value
               (str "client-ip : " (get-in request [:remote-addr]) " "
                    {:method-request (get-in request [:method-request])
                     :response response}))
  response)

(defn adjust-response
  "enable every option has opportunity to adjust response or do    some side-effects according response,
   return new response, handle reverse order of the options"
  [cmd request response]
  (loop [options (reverse (:options cmd))
         resp_ response]
    (let [[option-key option-value] (first options)]
      (if (nil? option-key)
        resp_
        (recur (rest options)
               (render-response option-key option-value request resp_))))))
