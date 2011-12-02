# clj-rpc

Version 0.2.0 Release.

A simple clojure rpc using clojure protocol 

all the arguments of the function and the result must statify the
rule:

(= form (f-decode (f-encode form)))

if use clj protocol
f-encode/f-decode => pr-str/read-string

if use json protocol (using cheshire json)
f-encode/f-decode => generate-string/parse-string  

## request and response.
* clj-rpc supports single invoke rpc and multi invoke prc.
* the request and response using json-rpc alike message
* request and reply messages both are put in http body. using utf-8 encoding.

### single invoke
* request : 
 
    {:method method-name :params [param1 param2 ...] :id request-id}

* response: (when error is nil then the rpc invoke is successful,
          otherwise fail)

    {:result execute-result :id request-id :error {:code 401 :message
  "some message" :data other-data-related-error}}

### multi invoke 
* requests : [request1 request2 ...]
* response : [response1 response2 ...]

## url
the http url format like

  http://host:port/[clj | json]/invoke?token=client-token

token: this is optional, the meaning of token will be explained below.

## export command

When we export a command, that mean we add a function that client can
remote invoke it.

define:

(export-commands ns fn-names & options)

example: 

```clojure
(export-commands 'clojure.core ["+" "-"] 
   {:require-context true :params-checks {0 [:number1]} })
```

this example mean we export a function + - of the clojure.core

And the client must supply a token that can get context, and
the (get-in context [:username]) must equal the first parameter of
invoke.

The context token and params check will explain below.

## context and params check

* clj-rpc server will use a token to identify the client. 
* every token specify a unique client.
* Token can be passed to server by query token parameter or cookie.

When we start a clj-rpc server we can sepcific get-context function,
that is a function that can get context of the client by the token. like
(get-context token). You can see the example in the section Usage. 

When we export a command, we can add options context check and params check
according to the context that we can get by the client token.

Examples:

Provided we have exported commands like above in the section export commands

we have a get-context function like below,

```clojure 
(defn my-get-context [token]
  (get {"token1" {:number1 1} "token2" {:number2 2}}))
```

If we remote invoke function like (+ 1 3) with token3,

then we will get a error, because token3 can not get a context.

and if we invoke function like (+ 2 3) with token1

then we will get a error two. 
because (not= 2 (get-in [:number1] {:number1 1}))

and if we invoke function like (+ 1 3) with token1, 

then we will get a correct answer 4.

## user data

Sometime , we need to access data (like session) related the connection.

Because this is a rpc invoke procedure, so when programmer wirtes
functions we assume them do not and we do not want them to know anything
about session in order to seperate the logic code and web framework. 

So clj-rpc.user-data supplies three functions to let programmer can
access connection related data.

* (save-user-data! data) return data 
* (get-user-data!) 
* (delete-user-data) return nil

Notice: this three functions should be used in the function that will
be exported. 

```clojure
(ns rpc.demo
  (:require [clj-rpc.user-data :as store]))

;;example function to using save-user-data!
(defn fn-save-data 
  []
  (store/save-user-data! {:save-time (System/currentTimeMillis)}))

;;example function to using get-user-data!
(defn fn-get-data
  []
  (store/get-user-data!))

;;example function to using delete-user-data!
(defn fn-delete-data
  []
  (store/delete-user-data!))
``` 
## Usage 

add to porject.clj

```clojure
[clj-rpc "0.2.0"]
```

sample code:

```clojure
(ns rpc.demo
 (:require [clj-rpc.server :as server]
           [clj-rpc.client :as client]))
 ;=============server code ===============

 ;;this is a function we want to export, 
 ;;but we want the sepcific user can invoke this function.
 (defn user-info 
    [username])

 ;;this is a get-context function.
 (defn get-context [token]
    (get {"token1" {:username "user1"}} token))

 ;;export user-info function
 (server/export-commands 'rpc.demo ["user-info"]
     {:require-context true :params-checks {0 [:username]} })    
 
 ;;export all functions in the namespace clojure.core
 (server/export-commands 'clojure.core nil)
 ;;export other namespace
  
 ;;start http server, the "clj-rpc-cookie" just a example value.
 (server/start {:fn-get-context get-context 
                :token-cookie-key "clj-rpc-cookie-key" }) 

 ;==============client code ===============
 ;; Obtain handle to endpoint
 (def endp (client/rpc-endpoint :server "localhost"))

 ;; get all export commands
 (client/help endp)
 
 ;; invoke some command
 ;; remote invoke the function (str "hello," "world") and return the result
 (client/invoke-rpc endp "str" ["hello, " "world"])

 ;;invoke the user-info function correctly.
 (client/invoke-rpc-with-token endp "token1" "user-info" ["user1"])  

```

You can use leiningen plugin [lein-clj-rpc](https://github.com/zhuangxm/lein-clj-rpc) to generate client stub code

## License

Copyright (C) 2011 FIXME

Distributed under the Eclipse Public License, the same as Clojure.

