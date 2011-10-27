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
            [clj-rpc.wire-format :as protocol]))

(def rpc-default-port 9876)

;;use dynamic method to export commands
(defonce commands (atom {}))

(defn export-commands
  "export all functions or specify functions in the namespace ns
   for example :
   (export-commands 'clojure.core)
   (export-commands \"clojure.core\")
   (export-commands 'clojure.core '+)
   (export-commands 'clojure.core \"+\")"  
  [ns & fn-names]
  (let [ns (symbol ns)]
    (require ns)
    (let [var-fns (map #(find-var (symbol (str ns "/" %))) fn-names)]
      (swap! commands merge 
             (apply command/get-commands ns var-fns)))))

(defn execute-method
  "get the function from the command-map according the method-name and
   execute this function with args
   return the execute result"
  [command-map method-name args]
  (logging/debug "execute-method == method-name: " method-name " args: " args)
  (when-let [f (command/.func (command-map method-name))]
    (apply f args)))

(defn help-commands
  "return the command list"
  [commands]
  (->> (vals commands)
      (map #(dissoc % :func))
      (sort-by #(:title %))))

(defroutes main-routes
  (ANY "/:s-method/help" [s-method]
       (when-let [[f-encode] (protocol/serialization s-method)]
         (f-encode (help-commands @commands))))
  (POST "/:s-method/invoke" [s-method method args]
        (logging/debug "invoking (" s-method ") method: " method " args: " args)
        (let [[f-encode f-decode] (protocol/serialization s-method)]
          (f-encode (execute-method @commands method (f-decode args)))))
  (route/not-found "invalid url"))

;;define a jetty-instance used to start or stop
(defonce jetty-instance (atom nil))

(defn stop
  "stop jetty server"
  []
  (when @jetty-instance 
    (.stop @jetty-instance))
  (reset! jetty-instance nil))

(defn start
  "start jetty server
   options : same as run-jetty of ring jetty adaptor"
  ([]
     (start {:join? false :port rpc-default-port :host "127.0.0.1"} ))
  ([options]
     (if @jetty-instance (stop))
     (reset! jetty-instance
             (run-jetty (handler/site main-routes) options))))
