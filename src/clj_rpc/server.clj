(ns clj-rpc.server
  (:use compojure.core, ring.adapter.jetty)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [clj-rpc.command :as command]))

;;TODO use dynamic method to export commands
(defonce commands (command/get-commands 'clj-rpc.command))

(defn execute-method
  "get the function from the command-map according the method-name and
   execute this function with args
   return clojure string of the execute result"
  [command-map method-name args]
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
  (GET "/help" [] (help-commands commands))
  (POST "/help" [] (help-commands commands))
  (POST "/invoke" {:keys [method args]}
        (execute-method commands method (read-string args)))
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
  "start jetty server"
  ([]
     (start false))
  ([joinable]
     (if @jetty-instance (stop))
     (reset! jetty-instance
          (run-jetty (handler/api main-routes)
                     {:port 8080 :join? joinable}))))


(defn -main
  [args]
  (start true))
