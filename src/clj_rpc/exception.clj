(ns clj-rpc.Exception
  (:gen-class
   :extends java.lang.RuntimeException
   :prefix "-"
   :init  "init"
   :state state
   :methods [[getCode [] java.lang.Long] [getData [] java.lang.Object]]
   :constructors {[Long String] [String] [Long Object String] [String]}))

(defn -init
  ([code msg]
               [[msg] (atom {:code code})])
  ([code data msg]
                [[msg] (atom {:code code :data data})]))

(defn -getCode [this] (:code @(.state this)))

(defn -getData [this] (:data @(.state this)))
