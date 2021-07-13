(ns pcp.scgi
    (:require [clojure.string :as str]
              [aleph.tcp :as tcp]
              [manifold.deferred :as d]
              [manifold.stream :as s]
              [byte-streams :as bs]
              [aleph.flow :as flow])
    (:import  [java.nio ByteBuffer]
              [java.util.concurrent ExecutorService])            
  (:gen-class))


(defn to-byte-array [^String text]
  (-> text (.getBytes "UTF-8") ByteBuffer/wrap))

(defn extract-headers [req]
  (let [header (str/replace req "\0" "\n")
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

(defn create-scgi-string [resp]
  (let [nl "\r\n"
        response (str (:status resp) nl (apply str (for [[k v] (:headers resp)] (str k ": " v nl))) nl (:body resp))]
    response))

(defn handle [handler msg]
  (-> msg 
      bs/to-string 
      extract-headers 
      handler 
      create-scgi-string 
      to-byte-array)) 

(defn processor [handler executor]
  (fn [s _]
    (->
      (s/take! s ::none)
      (d/onto executor)
      (d/chain
        (fn [msg]
          ;; (println msg)
          (d/future
            (handle handler msg)))
        (fn [msg']
          (when msg'
            (s/put! s msg'))))
      (d/catch
        (fn [ex]
          (println (str "ERROR: " ex))
          (println ex)))
      (d/finally
        (fn []
          (s/close! s))))))

(defn serve [handler port]
  (let [executor (flow/utilization-executor 0.9)
        server (tcp/start-server (processor handler executor) {:port port})]
    (println "running...")
    (fn [] 
      (.close server)
      (.shutdown ^ExecutorService executor))))



