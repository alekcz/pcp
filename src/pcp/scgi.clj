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
  (let [data-partial (str/replace req #"\u0000\u0000\u0000" "")
        len-string (re-find #"(\d*):" data-partial)
        _ (println len-string)
        len (+ 1 (Integer/parseInt (or (second len-string) "0")) (count (first len-string)))  
        _ (println len-string len)
        header-clean (subs data-partial (count (first len-string)) (- len 1))
        body (subs data-partial len)
        header (str/replace header-clean  "\0" "\n")
        vec-data (str/split header #"\n")
        raw-headers (butlast vec-data)
        keys (map #(-> % (str/replace "_" "-") str/lower-case keyword) (take-nth 2 raw-headers))
        values (take-nth 2 (rest raw-headers))
        headers (zipmap keys values)
        request (assoc headers :body body)]
    (spit "header.txt" header)
    (spit "body.txt" body)
    request))

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
    (while (some? @active)
      (if (not= 0 (.select selector 50))
          (let [keys (.selectedKeys selector)]      
            (doseq [^SelectionKey key keys]
              (let [ops (.readyOps key)]
                (cond
                  (= ops SelectionKey/OP_ACCEPT) (withAccept key)
                  (= ops SelectionKey/OP_READ)   (withRead key handler))))
            (.clear keys))
            nil))))

