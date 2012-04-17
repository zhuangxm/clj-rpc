(ns clj-rpc.exception)

(gen-class
    :name clj_rpc.CodeException
    :extends java.lang.RuntimeException
    :prefix "-"
    :init  "init"
    :state state
    :methods [[getCode [] java.lang.Long]]
    :constructors {[Long String] [String]})

(defn -init [code msg]
  [[msg] (atom {:code code})])

(defn -getCode [this] (:code @(.state this)))

(compile 'clj_rpc.CodeException)

(defn raise-error
  [code msg]
  (throw (clj_rpc.CodeException. code msg)))
