(ns pcp.scgi
    (:require [clojure.string :as str]
              [aleph.tcp :as tcp]
              [manifold.deferred :as d]
              [manifold.stream :as s]
              [byte-streams :as bs]
              [aleph.flow :as flow])
    (:import  [java.nio ByteBuffer]
              [java.util.concurrent ExecutorService]
              [java.io ByteArrayInputStream])            
  (:gen-class))


(defn to-byte-array [^String text]
  (-> text (.getBytes "UTF-8") ByteBuffer/wrap))

(defn to-string [str-as-bytes]
  (bs/to-string str-as-bytes))

(defn extract-headers [header]
  (let [data (str/split header #"\u0000")
        keys (map #(-> % (str/replace "_" "-") str/lower-case keyword) (take-nth 2 data))
        values (take-nth 2 (rest data))
        h (transient (zipmap keys values))]
    ;make the ring linter happy.
    (-> h
      (assoc! :server-port (Integer/parseInt (if (str/blank? (:server-port h)) "0" (:server-port h))))
      (assoc! :content-length (Integer/parseInt (if (str/blank? (:content-length h)) "0" (:content-length h))))
      (assoc! :request-method (-> (:request-method h) str str/lower-case keyword))
      (assoc! :headers {  "sec-fetch-site" (-> h :http-sec-fetch-site)   
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
      (assoc! :uri (:request-uri h))
      (assoc! :scheme (-> h :request-scheme keyword))
      (persistent!))))

(defn create-scgi-string [resp]
  (let [nl "\r\n"
        response (str (:status resp) nl (apply str (for [[k v] (:headers resp)] (str k ": " v nl))) nl (:body resp))]
    response))

(defn handle [handler msg]
  (-> msg
      handler 
      create-scgi-string 
      to-byte-array)) 


(defn size [bts]
  (let [seg (to-string (byte-array (vec (take 50 bts))))
        header-len (str/replace seg #":.*" "")
        len (count header-len)
        header-int (Integer/parseInt (if-not (str/blank? header-len) header-len "0" ))
        body (re-find #"(\d+)" (subs seg (inc len)))
        body-int (Integer/parseInt (if-not (str/blank? (second body)) (second body) "0"))]
    {:drop (inc len)
     :header header-int
     :body body-int
     :end (+ len header-int 1 1)
     :total (+ body-int header-int len 1 1)}))

(defn str+bin [string binary]
  (str string (bs/to-string binary)))

(defn processor [handler executor]
  (fn [s _]
    (let [null ::none]
      (->
        (s/take! s null)
        (d/onto executor)
        (d/chain
          (fn [msg]
            (let [meta (size msg) 
                  dmsg (d/deferred)
                  total (:total meta)] 
              (d/success! dmsg msg)
              (d/loop [m dmsg
                      bin []
                      done 0]
                (let [progress (+  done (count @m))]
                  (if (>= progress total)
                    [meta (mapcat seq (conj bin @m))]
                    (d/recur (s/take! s null) (conj bin @m) progress))))))
          (fn [res]
            (let [meta (first res)
                  msg (second res)
                  main (drop (:drop meta) msg)
                  header' (-> (:header meta) (take main) byte-array bs/to-string)
                  header (extract-headers header')
                  body (drop (:end meta) msg)
                  req (assoc header :body (ByteArrayInputStream. (byte-array body)))]
              (handle handler req)))
          (fn [msg']
            (when msg'
              (s/put! s msg'))))
        (d/catch
          (fn [ex]
            (println (str "ERROR: " ex))
            (println ex)))
        (d/finally
          (fn []
            (s/close! s)))))))

(defn serve [handler port]
  (let [executor (flow/utilization-executor 0.9)
        server (tcp/start-server (processor handler executor) {:port port})]
    (println "running...")
    (fn [] 
      (.close server)
      (.shutdown ^ExecutorService executor))))



