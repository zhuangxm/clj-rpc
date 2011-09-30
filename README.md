# clj-rpc

A simple clojure rpc using clojure protocol 

all the arguments of the function and the result must statify the
rule:

(= form (read-string (pr-str form))) 

## Usage 

```clojure
(ns rpc.demo
 (:require [clj-rpc.server :as server]
           [clj-rpc.client :as client]))


 ;=============server code ===============
 ;;export functions in the namespace
 (server/export-command 'clojure.core)
 ;;export other namespace
  
 ;;start http server
 (server/start) 

 ;==============client code ===============
 ;; get all export commands
 (client/help "http://localhost:8080/help)
 
 ;; invoke some command
 ;; remote invoke the function (str "hello," "world") and return the result
 (client/call "http://localhost:8080/invoke" "str" "hello, " "world")  

```

## Todo

* add some test
* check the compjure utf-8 problem.
* Can generate the client stub program according the help.

## License

Copyright (C) 2011 FIXME

Distributed under the Eclipse Public License, the same as Clojure.

