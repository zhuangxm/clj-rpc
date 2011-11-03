(ns clj-rpc.command
  (:require [clj-rpc.wire-format :as protocol]))

;;abstract the concept command that can be executed by client
;;command include command string, command doc, command arglists
(defrecord Command [name func doc arglists])

(defn var-fn?
  "check whether a var is a function.
   if the var has not been bound, then will throw a exception. so catch it "
  [x]
  (try (fn? (var-get x))
       (catch Exception e)))

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

(defn modify-func->cmd
  "Returns a pair of [name command] where command contains a
   f-modifier modified function."
  [f-modifier the-var & new-meta]
  {:pre [(fn? f-modifier) (var? the-var)]}
  (let [func (f-modifier (var-get the-var))
        new-meta (merge (meta the-var) new-meta)
        name (str (:name new-meta))
        cmd (Command. name func (:doc new-meta) (:arglists new-meta))]
    [name cmd]))

(def func->web-cmd
  (partial modify-func->cmd func->web))

(defn ns-funcs
  "Returns f-var processed functions in the ns, pred can filter the functions"
  ([f-var pred ns]
     (for [[var-sym the-var] (ns-publics ns)
             :when (and (var-fn? the-var) (pred var-sym))]
       (f-var the-var))))

(def ns-web-cmds
  "Returns web-cmds in a ns"
  (partial ns-funcs func->web-cmd))