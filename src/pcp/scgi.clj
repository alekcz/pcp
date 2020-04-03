(ns pcp.scgi
  (:require [clojure.java.io :as io]
            [com.climate.claypoole :as cp]
            [pcp.resp :as resp]
            [clojure.string :as str])
  (:import  (java.net Socket ServerSocket SocketException InetAddress)
            (java.io BufferedWriter))
  (:gen-class))

(set! *warn-on-reflection* 1)

(def pool (cp/threadpool 100))

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
        (cp/future pool
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
       (cp/shutdown pool)))

  