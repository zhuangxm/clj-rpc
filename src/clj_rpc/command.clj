(ns clj-rpc.command)


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

(defn get-commands 
  "get the specify var functions as commands in the namespace ns
   if the var-fns is null then get all public functions as commands. 
  return a map that key is function-name and value is a command"
  [ns & var-fns]
  (into {}
        (for [[var-sym the-var] (ns-publics ns)
              :when (and (var-fn? the-var)
                         (or (nil? (seq var-fns))
                             (some (partial = the-var) var-fns)))
              :let [var-name (str var-sym)]]
          [var-name (mk-command var-name the-var)])))

