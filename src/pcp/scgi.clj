(ns pcp.scgi
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import  (java.net Socket ServerSocket)
            (java.io BufferedWriter))
  (:gen-class))
  
(defn to-byte-array [text]
  (.getBytes ^String text "UTF-8"))

(defn to-string [bytes]
  (String. ^"[B" bytes "UTF-8"))

(def ans (to-byte-array "Status: 200 OK\r\nContent-Type: text/plain\r\n\r\nresponse\r\n"))

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
        bufsize 8192
        buf (byte-array bufsize)
        _ (.read is buf)]
    buf))

(defn send! [^Socket socket ^String msg]
  (let [^BufferedWriter writer (io/writer socket)] 
    (.write writer msg (int 0) (count msg))
    (.flush writer)))

(defn serve [port handler]
  (with-open [server-sock (ServerSocket. port)]
    (while server-sock              
      (let [^Socket sock (.accept server-sock)]
        ;(future
          (let [msg-in (receive! sock) msg-out (-> msg-in cleaner handler)]
            (send! sock msg-out)
            (.close ^Socket sock))))))
            ;)