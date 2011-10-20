(ns clj-rpc.server
  (:use compojure.core, ring.adapter.jetty)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [clj-rpc.command :as command]
            [clojure.tools.logging :as logging]
            [cheshire.core :as json]))

;;use dynamic method to export commands
(defonce commands (atom {}))

;;define protocol handle function
;;TODO this method is not so good.
(def ^:dynamic *read-function* read-string)
(def ^:dynamic *write-function* pr-str)

(defn export-commands
  "export all functions in the namespace ns"
  [ns]
  (require ns)
  (swap! commands merge 
         (command/get-commands ns)))

(defn execute-method
  "get the function from the command-map according the method-name and
   execute this function with args
   return clojure string of the execute result"
  [command-map method-name args]
  (logging/debug "execute-method == method-name: " method-name " args: " args)
  (when-let [f (command/.func (command-map method-name))]
    (*write-function* (apply f args))))

(defn help-commands
  "return clojure string of the command list"
  [commands]
  (->> (vals commands)
      (map #(dissoc % :func))
      (sort-by #(:title %))
      (*write-function*)))

(defroutes main-routes
  (GET "/help" [] (help-commands @commands))
  (POST "/help" [] (help-commands @commands))
  (POST "/invoke" [method args]
        (logging/debug "received invoke request == method: " method " args: " args)
        (execute-method @commands method (*read-function* args)))
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
          json-tag "/json/"]
      (if (.startsWith uri json-tag )
        (binding [*read-function* json/parse-string
                  *write-function* json/generate-string]
          (handler (assoc request :uri (change-json-uri uri json-tag))))
        (handler request))) ))

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

