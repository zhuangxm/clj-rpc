(ns clj-rpc.server
  (:use compojure.core, ring.adapter.jetty, compojure.response
        [ring.util.response :only (response content-type)])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [clj-rpc.command :as command]
            [clojure.tools.logging :as logging]))

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
   return clojure string of the execute result"
  [command-map method-name args]
  (logging/debug "execute-method == method-name: " method-name " args: " args)
  (when-let [f (command/.func (command-map method-name))]
    (pr-str (apply f args))))

(defn help-commands
  "return clojure string of the command list"
  [commands]
  (->> (vals commands)
      (map #(dissoc % :func))
      (sort-by #(:title %))
      (pr-str)))

(defroutes main-routes
  (GET "/help" [] (help-commands @commands))
  (POST "/help" [] (help-commands @commands))
  (POST "/invoke" [method args]
        (logging/debug "received invoke request == method: " method " args: " args)
        (execute-method @commands method (read-string args)))
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
     (start {:join? false :port 8080 :host "127.0.0.1"} ))
  ([options]
     (if @jetty-instance (stop))
     (reset! jetty-instance
             (run-jetty (handler/api main-routes) options))))


(defn -main
  [args]
  (start {:join? true}))
