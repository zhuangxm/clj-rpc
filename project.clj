(defproject clj-rpc "0.3.2"
            :description "simple rpc using clojure"
            :dependencies [[org.clojure/clojure "1.4.0"]
                           [org.clojure/tools.logging "0.2.3"]
                           [clj-http "0.7.8"]
                           [compojure "1.1.6"]
                           [ring "1.2.1"]
                           [cheshire "5.2.0"]
                           [easyconf "0.1.1"]]
            :dev-dependencies [[log4j "1.2.16"]
                               [midje "1.3-alpha5"]])
