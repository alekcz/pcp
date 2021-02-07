(ns pcp.scgi
  (:require [clojure.string :as str])
  (:import [java.nio.channels ServerSocketChannel SocketChannel Selector SelectionKey]
           [java.nio ByteBuffer]
           [java.net InetSocketAddress InetAddress]
           [java.io ByteArrayInputStream ByteArrayOutputStream])
  (:gen-class))

(set! *warn-on-reflection* 1)

(def ^Selector selector (Selector/open))

(defn to-byte-array [^String text]
  (-> text (.getBytes "UTF-8") ByteBuffer/wrap))

(defn extract-headers [req]
  (let [header-clean (:header req)
        header (str/replace header-clean  "\0" "\n")
        data (str/split header #"\n")
        keys (map #(-> % (str/replace "_" "-") str/lower-case keyword) (take-nth 2 data))
        values (take-nth 2 (rest data))
        h (zipmap keys values)]
    ;make the ring linter happy.
    (-> h
      (update :server-port #(Integer/parseInt (if (str/blank? %) "0" %)))
      (update :content-length #(Integer/parseInt (if (str/blank? %) "0" %)))
      (update :request-method #(-> % str str/lower-case keyword))
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
      (assoc :body (:body req)))))

(defn on-accept [^SelectionKey key]
  (let [^ServerSocketChannel channel     (.channel key)
        ^SocketChannel socketChannel   (.accept channel)]
    (.configureBlocking socketChannel false)
    (.register socketChannel selector SelectionKey/OP_READ)))

(defn create-scgi-string [resp]
  (let [nl "\r\n"
        response (str (:status resp) nl (apply str (for [[k v] (:headers resp)] (str k ": " v nl))) nl (:body resp))]
    response))

(defn on-read [^SelectionKey key handler]
  (let [^SocketChannel socket-channel (.channel key)]
    (try
      (let [buf (ByteBuffer/allocate 1)
            real-buf (ByteBuffer/allocate 16384)
            len-out (ByteArrayOutputStream.)
            header-out (ByteArrayOutputStream.)
            body-out (ByteArrayOutputStream.)]
        (.clear buf)
        ;Saving reqs
        ; (loop [len (.read socket-channel real-buf)]
        ;     (when (> len 0)
        ;       (.write body-out (.array real-buf) 0 len)
        ;       (.clear real-buf)
        ;       (recur (.read socket-channel real-buf)))) 
        ; (let [f (java.io.FileOutputStream. "test-resources/json.bin")
        ;       byties (.toByteArray body-out)]
        ;   (.write f byties 0 (count byties))
        ;   (.close f))
        (loop [_ (.read socket-channel buf)]
          (when (not= (-> buf .array String.) ":")
            (.write len-out (.array buf) 0 1)
            (.clear buf)
            (recur (.read socket-channel buf))))
        (let [maxi (try (Integer/parseInt (.toString len-out "UTF-8")) (catch Exception _ 0))]         
          (.clear buf)
          (loop [read 0 len (.read socket-channel buf)]
            (when (< read maxi)
              (.write header-out (.array buf) 0 len)
              (.clear buf)
              (recur (+ read len) (.read socket-channel buf)))))              
        (let [header (.toString header-out "UTF-8")]  
          (loop [len (.read socket-channel real-buf)]
            (when (> len 0)
              (.write body-out (.array real-buf) 0 len)
              (.clear real-buf)
              (recur (.read socket-channel real-buf))))   
              (let [^ByteBuffer resp (-> {:header header :body (ByteArrayInputStream. (.toByteArray body-out))} extract-headers handler create-scgi-string to-byte-array)]
                (.write socket-channel resp)
                (.close socket-channel)
                (.cancel key))))
      (catch Exception e (.close socket-channel) (.cancel key) (.printStackTrace e)))))

(defn build-server [port]
  (let [^ServerSocketChannel serverChannel (ServerSocketChannel/open)
        portAddr (InetSocketAddress. ^InetAddress (InetAddress/getByName "127.0.0.1") ^Integer port)]
      (.configureBlocking serverChannel false)
      (.bind (.socket serverChannel) portAddr)
      (.register serverChannel selector SelectionKey/OP_ACCEPT)
      serverChannel))

(defn serve [handler port]
  (let [active (atom true)
        ^ServerSocketChannel server (build-server port)]
    (future
      (while (some? @active)
        (if (not= 0 (.select selector 50))
            (let [keys (.selectedKeys selector)]      
              (doseq [^SelectionKey key keys]
                (let [ops (.readyOps key)]
                  (cond
                    (= ops SelectionKey/OP_ACCEPT) (on-accept key)
                    (= ops SelectionKey/OP_READ)   (on-read key handler))))
              (.clear keys))
              nil)))
    (fn [] 
      (.close server)
      (reset! active false))))

