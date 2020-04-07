(ns pcp.scgi
  (:require [com.climate.claypoole :as cp]
            [clojure.string :as str])
  (:import [java.nio.channels ServerSocketChannel SocketChannel Selector SelectionKey]
           [java.nio ByteBuffer]
           [java.net InetSocketAddress InetAddress])
  (:gen-class))

(set! *warn-on-reflection* 1)

(def ^Selector selector (Selector/open))
(def pool (cp/threadpool 100))

(defn to-byte-array [^String text]
  (-> text (.getBytes "UTF-8") ByteBuffer/wrap))

(defn to-string [^ByteBuffer buf]
  (-> buf .array String.))

(defn extract-headers [req]
  (let [data (-> req (str/replace "\0" "\n") (str/replace #",$" ""))
        vec-data (str/split data #"\n")
        keys (map #(-> % (str/replace "_" "-") str/lower-case keyword) (take-nth 2 vec-data))
        values (take-nth 2 (rest vec-data))]
    (zipmap keys values)))

(defn withAccept [^SelectionKey key]
  (let [^ServerSocketChannel channel     (.channel key)
        ^SocketChannel socketChannel   (.accept channel)]
    (.configureBlocking socketChannel false)
    (.register socketChannel selector SelectionKey/OP_READ)))

(defn withRead [^SelectionKey key handler]
  (try
    (let [^SocketChannel socket-channel (.channel key)
          buf (ByteBuffer/allocate 4096)]
      (.clear buf)
      (.read socket-channel buf)
      (.flip buf)   
      (let [^ByteBuffer resp (-> buf to-string extract-headers handler to-byte-array)]
        (.write socket-channel resp)
        (.close socket-channel)
        (.cancel key)))
    (catch Exception e (.printStackTrace e))))

(defn build-server [port]
  (let [^ServerSocketChannel serverChannel (ServerSocketChannel/open)
        portAddr (InetSocketAddress. (InetAddress/getByName "127.0.0.1") (int port))]
      (.configureBlocking serverChannel false)
      (.bind (.socket serverChannel) portAddr)
      (.register serverChannel selector SelectionKey/OP_ACCEPT)))

(defn serve 
  ([handler port] 
    (serve handler port (atom true)))
  ([handler port active]
    (build-server port)
    (future
      (while (some? @active)
        (if (not= 0 (.select selector 50))
            (let [keys (.selectedKeys selector)]      
              (doseq [^SelectionKey key keys]
                (if (= SelectionKey/OP_ACCEPT (.readyOps key)) (withAccept key) (withRead key handler)))
              (.clear keys))
              nil)))))

