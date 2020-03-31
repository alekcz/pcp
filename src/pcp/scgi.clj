(ns pcp.scgi
  (:require [com.climate.claypoole :as cp]
            [clojure.string :as str])
  (:import [java.nio.channels ServerSocketChannel Selector SelectionKey]
           [java.nio ByteBuffer CharBuffer]
           [java.net InetSocketAddress StandardSocketOptions])
  (:gen-class))

(def selector (Selector/open))
(def pool (cp/threadpool 100))

(defn to-byte-array [^String text]
  (-> text (.getBytes "UTF-8") ByteBuffer/wrap))

(defn to-string [buf]
  (-> buf .array String.))

(defn extract-headers [req]
  (let [data (-> req (str/replace "\0" "\n") (str/replace #",$" ""))
        vec-data (str/split data #"\n")
        keys (map #(-> % (str/replace "_" "-") str/lower-case keyword) (take-nth 2 vec-data))
        values (take-nth 2 (rest vec-data))]
    (zipmap keys values)))

(defn withAccept [key]
  (let [socketChannel (-> key .channel .accept)]
    (.configureBlocking socketChannel false)
    (.register socketChannel selector SelectionKey/OP_READ)))

(defn withRead [key handler]
  (try
    (let [socket-channel (.channel key)
        buf (ByteBuffer/allocate 8192)]
      (.clear buf)
      (.read socket-channel buf)
      (.flip buf)          
      (let [resp (-> buf to-string extract-headers handler to-byte-array)]
        (.write socket-channel resp)
        (.close socket-channel)
        (.cancel key)))
    (catch Exception e (.printStackTrace e))))

(defn withUnknown [key]
  (println "unkonwn type" (.readyOps key)))

(defn build-server [port]
  (let [serverChannel (ServerSocketChannel/open)
        portAddr (InetSocketAddress. port)]
      (.configureBlocking serverChannel false)
      (.bind (.socket serverChannel) portAddr)
      (.register serverChannel selector SelectionKey/OP_ACCEPT)
      serverChannel))

(defn serve [port handler]
  (let [serverChannel (build-server port)]
    (println "server started")
    (while true
      (if (not= 0 (.select selector 50))
          (let [keys (.selectedKeys selector)]      
            (doseq [key keys]
              (let [ops (.readyOps key)]
                (cond
                  (= ops SelectionKey/OP_ACCEPT) (withAccept key)
                  (= ops SelectionKey/OP_READ)   (withRead key handler)
                  :else (withUnknown key))))
            (.clear keys)))
          nil))
  (println "end"))

