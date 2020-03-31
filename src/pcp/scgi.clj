(ns pcp.scgi
  (:require [clojure.java.io :as io]
            ;s[com.climate.claypoole :as cp]
            [clojure.string :as str])
  (:import  (java.net Socket ServerSocket SocketException InetAddress)
            (java.io BufferedWriter))
  (:gen-class))

(set! *warn-on-reflection* 1)

;(def pool (cp/threadpool 10))

(defn to-byte-array [text]
  (.getBytes ^String text "UTF-8"))

(defn to-string [bytes]
  (String. ^"[B" bytes "UTF-8"))

(def ans (to-byte-array "Status: 200 OK\r\nContent-Type: text/plain\r\n\r\nresponse\r\n"))
(def ans2 (to-byte-array "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nHello\r\n"))

(defn extract-headers [req]
  (let [data (-> req (str/replace "\0" "\n") (str/replace #",$" ""))
        vec-data (str/split data #"\n")
        keys (map #(-> % (str/replace "_" "-") str/lower-case keyword) (take-nth 2 vec-data))
        values (take-nth 2 (rest vec-data))]
    (zipmap keys values)))

(defn cleaner [socket-data]
  (-> socket-data to-string extract-headers))
 
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