(ns pcp.utility
  (:require
    [pcp.resp :as resp]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [ring.adapter.simpleweb :as web])
  (:import  [java.net Socket]
            [java.io BufferedWriter]) 
  (:gen-class))

(set! *warn-on-reflection* 1)

(def root (atom "."))

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

(defn forward [scgi-req]
  (let [scgi-port (Integer/parseInt (or (System/getenv "SCGI_PORT") "9000"))
        socket (Socket. "127.0.0.1" scgi-port)]
        (send! socket scgi-req)
        (let [ans (-> socket receive! resp/to-string)]
          ans)))

(defn format-response [status body mime-type]
  (-> (resp/response body)    
      (resp/status status)
      (resp/content-type mime-type))) 

(defn file-response [path file]
  (let [mime (resp/get-mime-type (re-find #"\.[0-9A-Za-z]{1,7}$" path))]
    (-> (resp/response file)    
        (resp/status 200)
        (resp/content-type mime))))

(defn create-resp [scgi-response]
  (let [resp-array (str/split scgi-response #"\r\n")
        status (Integer/parseInt (first resp-array))
        body (str/join "\n" (-> resp-array rest rest))
        mime "text/html"
        final-resp (format-response status body mime)]
    final-resp))

(defn file-exists? [path]
  (-> path io/file .exists))
  
(defn serve-file [path]
  (file-response path (io/file path)))

(defn local-handler [request]
  (let [root (.getCanonicalPath (io/file @root))
        path (str root (:uri request))
        slashpath (str path "index.clj")
        exists (or (file-exists? path) (file-exists? slashpath))
        not-found (str root "/404.clj")
        full (assoc request 
                  :document-root root 
                  :document-uri (if (str/ends-with? (:uri request) "/") (str (:uri request) "index.clj") (:uri request)))]
      (cond 
        (and (str/ends-with? (:document-uri full) ".clj") exists)
          (-> full resp/http-to-scgi forward create-resp)
        exists 
          (serve-file path)
        (file-exists? not-found)
          (-> (assoc full :document-uri "/404.clj") resp/http-to-scgi forward create-resp)
        :else (format-response 404 nil nil))))


(defn start-local-server [port] 
  (println (str "Local server started on http://127.0.0.1:" port))
  (web/run-simpleweb local-handler {:port port}))

(defn -main 
  ([]
    (-main ""))
  ([path]       
    (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
      (case path
        "" (start-local-server port)
        "-v" (println "pcp v0.0.1-beta")
        "--version" (println "pcp v0.0.1-beta")
        (do
          (reset! root path)
          (start-local-server port))))))

      