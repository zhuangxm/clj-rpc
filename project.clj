(defproject clj-rpc "0.3.3"
  :description "simple rpc using clojure"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.namespace "0.2.5"]
                 [clj-http "0.7.8"]
                 [compojure "1.1.6"]
                 [ring "1.2.1"]
                 [cheshire "5.2.0"]
                 [easyconf "0.1.1"]]

  :profiles {:dev
             {:dependencies [[log4j "1.2.16"]
                             [midje "1.6.3"]]
              :plugins [[lein-midje "3.1.3"]]}})
