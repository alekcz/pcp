(ns pcp.scgi
  (:require [clojure.java.io :as io]
            ;[com.climate.claypoole :as cp]
            [clojure.walk :as walk]
            [pcp.resp :as resp]
            [clojure.string :as str])
  (:import  (java.net Socket ServerSocket SocketException InetAddress)
            (java.io BufferedWriter))
  (:gen-class))

(set! *warn-on-reflection* 1)

;(def pool (cp/threadpool 10))


(def ans (resp/to-byte-array "Status: 200 OK\r\nContent-Type: text/plain\r\n\r\nresponse\r\n"))
(def ans2 (resp/to-byte-array "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nHello\r\n"))

(defn extract-headers [req]
  (let [data (-> req (str/replace "\0" "\n") (str/replace #",$" ""))
        vec-data (str/split data #"\n")
        keys (map #(-> % (str/replace "_" "-") str/lower-case keyword) (take-nth 2 vec-data))
        values (take-nth 2 (rest vec-data))]
    (zipmap keys values)))

(defn cleaner [socket-data]
  (-> socket-data resp/to-string extract-headers))
 
 (defn receive! [socket]
  (let [is (io/input-stream socket)
        bufsize 4096
        buf (byte-array bufsize)
        _ (.read is buf)]
    buf))

(defn send! [^Socket socket ^String msg]
  (let [^BufferedWriter writer (io/writer socket)] 
    (.write writer msg (int 0) (count msg))
    (.flush writer)))

(defn accept-connection [server-sock handler]
  (let [^Socket sock (.accept ^ServerSocket server-sock)]
        (future
          (let [msg-in (receive! sock) msg-out (-> msg-in cleaner handler)]
            (send! sock msg-out)
            (.close ^Socket sock)))))

(defn serve [port handler]
    (with-open [server-sock (ServerSocket. port (int 150) (InetAddress/getByName "127.0.0.1"))]
      (loop [connections 1]      
        (try        
          (accept-connection server-sock handler)
          (catch SocketException _disconnect))
        (recur (inc connections)))
      ;(cp/shutdown pool)
      ))

(defn scgi [port handler]
    (with-open [server-sock (ServerSocket. port (int 150) (InetAddress/getByName "127.0.0.1"))]
      (loop [connections 1]      
        (try        
          (accept-connection server-sock handler)
          (catch SocketException _disconnect))
        (recur (inc connections)))
      ;(cp/shutdown pool)
      ))


(defn http-to-scgi [req]
  (let [header (walk/keywordize-keys (:headers req))]
    (str
      "REQUEST_METHOD\0" (-> req :request-method name str/upper-case)  "\n"
      "REQUEST_URI\0" (-> req :uri) "\n"
      "QUERY_STRING\0" (-> req :query-string) "\n"
      "CONTENT_TYPE\0" (-> req :content-type) "\n"
      "DOCUMENT_URI\0" (-> req :document-uri) "\n"
      "DOCUMENT_ROOT\0" (-> req :document-root) "\n"
      "SCGI\0" 1 "\n"
      "SERVER_PROTOCOL\0" (-> req :protocol) "\n"
      "REQUEST_SCHEME\0" (-> req :scheme) "\n"
      "HTTPS\0" (-> req :name) "\n"
      "REMOTE_ADDR\0" (-> req :remote-addr) "\n"
      "REMOTE_PORT\0" (-> req :name) "\n"
      "SERVER_PORT\0" (-> req :server-port) "\n"
      "SERVER_NAME\0" (-> req :server-name) "\n"
      "HTTP_CONNECTION\0" (-> header :connection) "\n"
      "HTTP_CACHE_CONTROL\0" (-> header :cache-control) "\n"
      "HTTP_UPGRADE_INSECURE_REQUESTS\0" (-> header :upgrade-insecure-requests) "\n"
      "HTTP_USER_AGENT\0" (-> header :user-agent) "\n"
      "HTTP_SEC_FETCH_DEST\0" (-> header :sec-fetch-dest) "\n"
      "HTTP_ACCEPT\0" (-> header :cookie) "\n"
      "HTTP_SEC_FETCH_SITE\0" (-> header :sec-fetch-site) "\n"
      "HTTP_SEC_FETCH_MODE\0" (-> header :sec-fetch-mode) "\n"
      "HTTP_SEC_FETCH_USER\0" (-> header :sec-fetch-user) "\n"
      "HTTP_ACCEPT_ENCODING\0" (-> header :accept-encoding) "\n"
      "HTTP_ACCEPT_LANGUAGE\0" (-> header :accept-language) "\n"
      "HTTP_COOKIE\0" (-> header :cookie) "\n"
      "\n,")))


  