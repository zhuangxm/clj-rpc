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
(defonce ^:dynamic *commands* (atom {}))

(defn export-commands
  "export all functions (fn-names is null or empty)
   or specify functions in the namespace ns
   invoker of this method must notice the order of the options
  options:    options is a collection include several keys
    like [ [:requre-context true] [:params-checks ...] [... ...] ]

  the meaning of option:
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
                    [ [:require-context true] [:params-check {0 [:id]}] ])"
  [ns fn-names & [options]]
  (let [ns (symbol ns)]
    (require ns)
    (let [var-fns (map #(find-var (symbol (str ns "/" %))) fn-names)]
      (swap! *commands* merge 
             (context/add-context-to-command-map
              (apply command/get-commands ns var-fns)
              options)))))

(defn execute-command
  "get the function from the command-map according the method-name and
   execute this function with args
   return the execute result"
  [command-map request method-request]
  (logging/debug "execute-command == " method-request)
  (let [cmd (command-map (:method method-request))
        f (and cmd (command/.func cmd))
        new-method-request (context/adjust-method-request
                            cmd request method-request)]
    (rpc/execute-method f new-method-request)))

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
  [command-map request rpc-request]
  (letfn [(fn-execute [r]
             (execute-command command-map request
                              (change-str->keyword r)))]
    (if (map? rpc-request)
      (fn-execute rpc-request)
      (map fn-execute rpc-request))))

(defroutes main-routes
  (ANY "/:s-method/help" [s-method]
       (when-let [[f-encode] (protocol/serialization s-method)]
         (f-encode (help-commands @*commands*))))
  (POST "/:s-method/invoke" [s-method :as reqeust]
        (let [rpc-request (slurp (:body reqeust))]
          (logging/debug "invoking (" s-method ") request: " rpc-request)
          (let [[f-encode f-decode] (protocol/serialization s-method)]
            (f-encode (rpc-invoke @*commands* reqeust (f-decode rpc-request))))))
  (route/not-found "invalid url"))

;;define a jetty-instance used to start or stop
(defonce jetty-instance (atom nil))

(defn stop
  "stop jetty server"
  ([]
     (stop @jetty-instance)
     (reset! jetty-instance nil))
  ([instance]
      (when instance 
        (.stop instance))))

(defn wrap-commands
  "enable binding a custom commands map
   new-commands must be a atom of map"
  [handler new-commands]
  (fn [request]
    (if new-commands
      (binding [*commands* new-commands]
        (handler request))
      (handler request))))

(defn build-hander [options]
  (-> main-routes
      (context/wrap-context (:fn-get-context options)
                            (:cookie-attrs options)
                            (:token-cookie-key options))
      (context/wrap-client-ip)
      (wrap-commands (:commands options))
      handler/site))

(defn start
  "start jetty server
   options :
      base on options of run-jetty  of ring jetty adaptor
      and add several more
      :fn-get-context  => function to get the context (fn-get-context token)
      :token-cookie-key => the cookie name according to the token
      :cookie-attrs => the cookie default attributes, include domain or others
                       reference ring wrap-session
      :commands => the custom commands (atom of map) to be exported. (optional) "
  ([]
     (start {:join? false :port rpc-default-port :host "127.0.0.1"} ))
  ([options]
     (let [jetty (run-jetty (build-hander options) options)]
       (if-not (:command options)
         (reset! jetty-instance jetty)
         jetty))))

(defmacro with-commands
  "if you want to define another commands, using this macro,
   and in it invoke export-commands function lile:"
  ;;define another command map.
  [a-commands & body]
  `(binding [*commands* ~a-commands]
    ~@body))

(comment
  "an example of how to start another server with different commands"
  ;;main server.
  (export-commands ... )
  (export-commands ... )
  (start {:join? false :port 9876})

  ;;if you want to stop default server
  (stop)

  ;;another server with differenct command
  (let [new-commands (atom {})]
    (with-commands new-commands
      (export-commands ...)
      (export-commands ...)))

  ;;start another server (can not be stop)
    
  (def another-server (start {:join? false :commands new-commands :port 1980}))

  ;;if you want to stop another server
  (stop another-server))
