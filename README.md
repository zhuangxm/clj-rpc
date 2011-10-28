# clj-rpc

A simple clojure rpc using clojure protocol 

all the arguments of the function and the result must statify the
rule:

(= form (read-string (pr-str form))) 

## Usage 

add to porject.clj

````clojure
[clj-rpc "0.1.0-SNAPSHOT"]
```

sample code:

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
 ;; Obtain handle to endpoint
 (def endp (client/rpc-endpoint :server "localhost"))

 ;; get all export commands
 (client/help endp)
 
 ;; invoke some command
 ;; remote invoke the function (str "hello," "world") and return the result
 (client/invoke-rpc endp "str" "hello, " "world")  

```

You can use leiningen plugin [lein-clj-rpc](https://github.com/zhuangxm/lein-clj-rpc) to generate client stub code

## License

Copyright (C) 2011 FIXME

Distributed under the Eclipse Public License, the same as Clojure.

