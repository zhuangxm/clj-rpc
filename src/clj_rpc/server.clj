;; Server for simple RPC(http)
;;
;; example usage:
;; (export-commands 'clojure.core)
;; to export all functions of clojure.core to outside world.
;; (start)
;; to start the http server on the default rpc port 9876
;; (stop)
;; to stop the server.
;;
;; Server urls:
;; The server supports different wire format (currently clj, json).
;; First part of URL specify it:
;; e.g. http://locahost:9876/json  json format
;; For each format, the server support 2 commands:
;; help -- GET/POST returns the list of the functions
;; invoke -- POST (parameters: method, args) invoke the function (by
;;"method")
(ns clj-rpc.server
  (:use compojure.core, ring.adapter.jetty)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [clj-rpc.command :as command]
            [clojure.tools.logging :as logging]
            [cheshire.core :as json]
            [clj-rpc.wire-format :as protocol]
            [clj-rpc.rpc :as rpc]
            [clj-rpc.context :as context]))

(def rpc-default-port 9876)


;;TODO add the ability to dynamic add wrap-context middleware
;;use dynamic method to export commands
(defonce commands (atom {}))

(defn export-commands
  "export all functions (fn-names is null or empty)
   or specify functions in the namespace ns

  options:  is a map include several keys
   :require-context (true or false default false)
      whether this command must have context
   :params-checks
     check parameters of the method be invoked statisfy the sepcific requirements
     example :
       {0 [:username]} -->
       the first parameter must equals (get-in context [:username])

   for example :
   (export-commands 'clojure.core nil)
   (export-commands \"clojure.core\" nil)
   (export-commands 'clojure.core ['+])
   (export-commands 'clojure.core [\"+\"]
                    {:require-context true :params-check {0 [:id]}})"
  [ns fn-names & [options]]
  (let [ns (symbol ns)]
    (require ns)
    (let [var-fns (map #(find-var (symbol (str ns "/" %))) fn-names)]
      (swap! commands merge 
             (context/add-context-to-command-map
              (apply command/get-commands ns var-fns)
              options)))))

(defn execute-command
  "get the function from the command-map according the method-name and
   execute this function with args
   return the execute result"
  [command-map context {:keys [method params id]}]
  (logging/debug "execute-command == method-name: "
                 method " params: " params " id: " id)
  (let [cmd (command-map method)
        f (and cmd (command/.func cmd))
        check-result (context/check-context cmd context params)]
    (if check-result
      (rpc/mk-error (:code check-result) id (:message check-result))
      (rpc/execute-method f params id))))

(defn help-commands
  "return the command list"
  [commands]
  (->> (vals commands)
      (map #(dissoc % :func))
      (sort-by #(:title %))))

(defn change-str->keyword
  [m]
  (into {} (map (fn [[key value]] [(keyword key) value]) m)))

(defn rpc-invoke
  "invoke rpc method
   rpc-request can a map (one invoke) or a collection of map (multi invokes)"
  [command-map context rpc-request]
  (letfn [(fn-execute [r]
             (execute-command command-map context
                              (change-str->keyword r)))]
    (if (map? rpc-request)
      (fn-execute rpc-request)
      (map fn-execute rpc-request))))

(defroutes main-routes
  (ANY "/:s-method/help" [s-method]
       (when-let [[f-encode] (protocol/serialization s-method)]
         (f-encode (help-commands @commands))))
  (POST "/:s-method/invoke" [s-method :as {context :context body :body}]
        (let [rpc-request (slurp body)]
          (logging/debug "invoking (" s-method ") request: " rpc-request)
          (let [[f-encode f-decode] (protocol/serialization s-method)]
            (f-encode (rpc-invoke @commands context (f-decode rpc-request))))))
  (route/not-found "invalid url"))

;;define a jetty-instance used to start or stop
(defonce jetty-instance (atom nil))

(defn stop
  "stop jetty server"
  []
  (when @jetty-instance 
    (.stop @jetty-instance))
  (reset! jetty-instance nil))

(defn build-hander [options]
  (handler/site
   (context/wrap-context  main-routes
                          (:fn-get-context options)
                          (:token-cookie-key options))))

(defn start
  "start jetty server
   options :
      base on options of run-jetty  of ring jetty adaptor
      and add two more
      :fn-get-context  => function to get the context (fn-get-context token)
      :token-cookie-key => the cookie name according to the token"
  ([]
     (start {:join? false :port rpc-default-port :host "127.0.0.1"} ))
  ([options]
     (if @jetty-instance (stop))
     (reset! jetty-instance
             (run-jetty (build-hander options) options))))
