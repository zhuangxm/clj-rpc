(ns clj-rpc.protocol
  (:require [cheshire.core :as json]))

(defprotocol Serialization
  "define how to serailizate clojure data to and from other data format"
  (decode [this data]
            "read string s into clojure data format")
  (encode [this clojure-data]
             "write clojure data to other data format"))


(deftype JsonSerialization []
  Serialization
  (decode [_ data]
          (if data 
            (json/parse-string data)))
  (encode [_ clojure-data]
          (json/generate-string clojure-data)))

(deftype ClojureSerialization []
  Serialization
  (decode [_ data]
          (if data 
            (read-string data)))
  (encode [_ clojure-data]
          (pr-str clojure-data)))


(defn mk-json-serialization
  "make a json serialization implement"
  []
  (JsonSerialization.))

(defn mk-clojure-serialization
  "make a json serialization implement"
  []
  (ClojureSerialization.))









