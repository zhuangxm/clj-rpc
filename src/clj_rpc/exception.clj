(ns clj-rpc.Exception
  (:gen-class
   :extends java.lang.RuntimeException
   :prefix "-"
   :init  "init"
   :state state
   :methods [[getCode [] java.lang.Long]]
   :constructors {[Long String] [String]}))

(defn -init [code msg]
  [[msg] (atom {:code code})])

(defn -getCode [this] (:code @(.state this)))
