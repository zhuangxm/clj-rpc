(ns clj-rpc.command
  (:require [clj-rpc.wire-format :as protocol]))

;;abstract the concept command that can be executed by client
;;command include command string, command doc, command arglists
(defrecord Command [name func doc arglists])

(defn mk-command 
  "make a command that can be executed
   cmd-str : string of the command
   var-f : the actutal execute function of the command"
  [cmd-str var-f]
  (let [m (meta var-f)]
    (Command. cmd-str var-f (:doc m) (str (:arglists m))))) 

(defn var-fn?
  "check whether a var is a function.
   if the var has not been bound, then will throw a exception. so catch it "
  [x]
  (try (fn? (var-get x))
       (catch Exception e)))

(defn filter-commands
  "get all the functions as commands in the namespace ns
   if statify (prd var-fn) is true 
  return a map that key is function-name and value is a command"
  [ns pred]
  (into {}
        (for [[var-sym the-var] (ns-publics ns)
              :when (and (var-fn? the-var) (pred the-var))
              :let [var-name (str var-sym)]]
          [var-name (mk-command var-name the-var)])))

(defn get-commands 
  "get the specify var functions as commands in the namespace ns
   if the var-fns is null then get all public functions as commands. 
  return a map that key is function-name and value is a command"
  ([ns]
     (filter-commands ns (constantly true)))
  ([ns & var-fns]
     (filter-commands ns (set var-fns))))

(defn wrap-invoke
  [handler]
  (fn [{{:keys [args s-method] :or {s-method "json"}} :params :as req}]
    (let [[f-encode f-decode] (protocol/serialization s-method)
          args (when args (f-decode args))]
      (-> (assoc req :args args) handler f-encode))))

(defn web-func
  "Returns a web function which transform a normal func by accept a
   ring request map as its input. We can also curry func by optional arg-paths
   which will take args from the request map.

   For example, (web-func + [:session :number])
   will produce a new function which get 1st argument from :number of session,
   and other arguments from :args of parameters."
  [func & arg-paths]
  {:pre [(fn? func) (every? vector? arg-paths)]}
  (fn [req]
    (let [fixed-args (map #(get-in req %) arg-paths)
          args (get req :args)]
      (apply func (concat fixed-args args)))))

(def func->web (comp wrap-invoke web-func))