(ns clj-rpc.server
  (:use compojure.core, ring.adapter.jetty)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [clj-rpc.command :as command]
            [clojure.tools.logging :as logging]
            [cheshire.core :as json]
            [clj-rpc.protocol :as protocol]))

;;use dynamic method to export commands
(defonce commands (atom {}))

(defn export-commands
  "export all functions in the namespace ns"
  [ns]
  (require ns)
  (swap! commands merge 
         (command/get-commands ns)))

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
  (GET "/help" [] (help-commands @commands))
  (POST "/help" [] (help-commands @commands))
  (POST "/invoke" [method args :as {serialization :serialization}]
        (logging/debug "received invoke request == method: " method " args: " args)
        (execute-method @commands method (protocol/decode serialization args)))
  (route/not-found "invalid url"))


(defn change-json-uri
  "change the url from /json/* to /* , remove the /json/ tag from url"
  [uri json-tag]
  (.substring uri (dec (.length json-tag))))

(defn wrap-protocol
  "support json protocol if the url is begin with /json/
  like /json/help or /json/invoke"
  [handler]
  (fn [request]
    (let [uri (:uri request)
          json-tag "/json/"
          json? (.startsWith uri json-tag)
          serialize-method (if json?
                             (protocol/mk-json-serialization)
                             (protocol/mk-clojure-serialization))
          new-uri (if json? (change-json-uri uri json-tag) uri)
          new-request (-> request
                           (assoc :uri new-uri :serialization serialize-method)) 
          response (handler new-request)]
      (update-in response [:body] (partial protocol/encode serialize-method) )) ))

;;define a jetty-instance used to start or stop
(defonce jetty-instance (atom nil))

(defn stop
  "stop jetty server"
  []
  (when @jetty-instance 
    (.stop @jetty-instance))
  (reset! jetty-instance nil))


(defn main-handler
  []
  (-> main-routes
      handler/api
      wrap-protocol))

(defn start
  "start jetty server
   options : same as run-jetty of ring jetty adaptor"
  ([]
     (start {:join? false :port 8080 :host "127.0.0.1"} ))
  ([options]
     (if @jetty-instance (stop))
     (reset! jetty-instance
             (run-jetty (main-handler) options))))
