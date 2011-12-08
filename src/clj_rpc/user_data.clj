(ns clj-rpc.user-data)

(defn uuid
  "生成唯一ID"
  []
  (str (java.util.UUID/randomUUID)))

;;define the user relate data.
(def atom-user-datas (atom {}))

;;define user relate token, used for binding to per request
(def ^:dynamic *atom-token* (atom nil))

(defn get-user-data
  "the default method to get user data"
  [token]
  (get @atom-user-datas token))

(defn get-user-data!
  "用户在正常方法中使用"
  []
  (get-user-data @*atom-token*))

(defn save-user-data!
  "存储用户数据，如果原来没有token，那么创建一个新的token
   返回 data"
  [data]
  (do
    ;;treat @atom-token* "" as nil
    (if (not (seq @*atom-token*)) (reset! *atom-token* (uuid)) )
    (swap! atom-user-datas assoc @*atom-token* data )
    data))

(defn delete-user-data!
  "删除用户相关数据,
  纯粹副作用，返回nil"
  []
  (do
    (swap! atom-user-datas dissoc @*atom-token*)
    (reset! *atom-token* "")
    nil))

;;TODO 增加用户数据定期清理
