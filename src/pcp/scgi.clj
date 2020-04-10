(ns pcp.scgi
  (:require [com.climate.claypoole :as cp]
            [clojure.string :as str]
            [clojure.java.io :as io])
  (:import [java.nio.channels ServerSocketChannel SocketChannel Selector SelectionKey]
           [java.nio ByteBuffer]
           [java.net InetSocketAddress InetAddress]
           [java.io ByteArrayInputStream])
  (:gen-class))

(set! *warn-on-reflection* 1)

(def ^Selector selector (Selector/open))
(def pool (cp/threadpool 100))

(defn to-byte-array [^String text]
  (-> text (.getBytes "UTF-8") ByteBuffer/wrap))

(defn to-string [^ByteBuffer buf]
  (-> buf .array String.))

(defn extract-headers [req]
  (let [len-string (re-find #"(\d*):" req)
        netring-len (count (first len-string))
        len (Integer/parseInt (or (second len-string) "0"))
        header-clean (subs req netring-len len)
        header (str/replace header-clean  "\0" "\n")
        vec-data (str/split header #"\n")
        body-start (+ 1 netring-len len)
        body-len (Integer/parseInt (or (second vec-data) "0"))
        content (subs req body-start (+ body-start body-len))
        raw-headers (butlast vec-data)
        keys (map #(-> % (str/replace "_" "-") str/lower-case keyword) (take-nth 2 raw-headers))
        values (take-nth 2 (rest raw-headers))
        h (zipmap keys values)]
    ;make the ring linter happy.
    (-> h
      (update :server-port #(Integer/parseInt (if (or (nil? %) (empty? %)) "0" %)))
      (update :content-length #(Integer/parseInt (if (or (nil? %) (empty? %)) "0" %)))
      (update :request-method #(-> % str/lower-case keyword))
      (assoc :headers { "sec-fetch-site" (-> h :http-sec-fetch-site)   
                        "host" (-> h :http-host)   
                        "user-agent" (-> h :http-user-agent)     
                        "cookie" (-> h :http-cookie)   
                        "sec-fetch-user" (-> h :http-sec-fetch-user)   
                        "connection" (-> h :hhttp-connection)   
                        "upgrade-insecure-requests" (-> h :http-sec-fetch-site)   
                        "accept"  (-> h :http-accept)   
                        "accept-language"   (-> h :http-accept-language)   
                        "sec-fetch-dest" (-> h :http-sec-fetch-dest)   
                        "accept-encoding" (-> h :http-accept-encoding)   
                        "sec-fetch-mode" (-> h :http-sec-fetch-mode)    
                        "cache-control" (-> h :http-cache-control)})
      (assoc :headers {})
      (assoc :uri (:request-uri h))
      (assoc :scheme (-> h :request-scheme keyword))
      (assoc :body (ByteArrayInputStream. (.getBytes content))))))

(defn on-accept [^SelectionKey key]
  (let [^ServerSocketChannel channel     (.channel key)
        ^SocketChannel socketChannel   (.accept channel)]
    (.configureBlocking socketChannel false)
    (.register socketChannel selector SelectionKey/OP_READ)))

(defn create-scgi-string [resp]
  (let [mime (-> resp :headers (get "Content-Type"))
        nl "\r\n"
        response (str (:status resp) nl (str "Content-Type: " mime) nl nl (:body resp))]
    response))

(defn on-read [^SelectionKey key handler]
  (try
    (let [^SocketChannel socket-channel (.channel key)
          buf (ByteBuffer/allocate 2097152)]
      (.clear buf)
      (.read socket-channel buf)
      (.flip buf)   
      (let [^ByteBuffer resp (-> buf to-string extract-headers handler create-scgi-string to-byte-array)]
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
                  (= ops SelectionKey/OP_ACCEPT) (on-accept key)
                  (= ops SelectionKey/OP_READ)   (on-read key handler))))
            (.clear keys))
            nil))))

