;; Different wire formats support.
(ns clj-rpc.wire-format
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]))

(defmulti serialization
  "Returns a pair of encoder, decoder function."
  (partial keyword "clj-rpc.wire-format"))

(defmethod serialization ::clj [_]
  [pr-str edn/read-string])

(defmethod serialization ::json [_]
  [json/generate-string json/parse-string])
