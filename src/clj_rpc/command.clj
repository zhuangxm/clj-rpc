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
  "get all public functions in the namespace ns
  return a map that key is function-name and value is a command"
  [ns]
  (->> (ns-publics ns)
      (filter #(var-fn? (second %)))
      (map #(vector (str (first %))
                    (mk-command (str (first %)) (second %))))
      (flatten)
      (apply hash-map)))

