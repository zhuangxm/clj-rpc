(defproject clj-rpc "0.2.5-SNAPSHOT"
            :description "simple rpc using clojure"
            :dependencies [[org.clojure/clojure "1.3.0"]
                           [org.clojure/tools.logging "0.2.3"]
                           [clj-http "0.2.1"]
                           [compojure "0.6.5"]
                           [ring "0.3.11"]
                           [cheshire "2.0.2"]]
            :dev-dependencies [[log4j "1.2.16"]
                               [midje "1.3-alpha5"]]
            :aot [clj-rpc.Exception])

