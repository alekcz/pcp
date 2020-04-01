(ns pcp.core
  (:require
    [sci.core :as sci]
    [pcp.resp :as resp]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [pcp.scgi :as scgi]
    [ring.adapter.simpleweb :as web]
    
    ;included in environment
    [cheshire.core :as json]
    [clostache.parser :as parser]
    [clj-http.lite.client :as client]
    [next.jdbc :as jdbc]
    [honeysql.core :as sql]
    [honeysql.helpers :as helpers]
    ;[postal.core :as email]
    )
  (:import [org.simpleframework.http Status]
           [org.simpleframework.http.core ContainerServer Container]
           [org.simpleframework.transport.connect Connection SocketConnection]
           [org.simpleframework.http Response Request]
           [java.net URLDecoder InetSocketAddress SocketAddress]
           [java.io PrintStream File InputStream FileInputStream]) 
  (:gen-class))

(set! *warn-on-reflection* 1)

(def admin-path "./admin")

(defn extract-namespace [namespace]
  (into {} (ns-publics namespace)))

(def namespaces
  { 
    ;'postal.core (extract-namespace 'postal.core)
    'clostache.parser (extract-namespace 'clostache.parser)
    'clj-http.lite.client (extract-namespace 'clj-http.lite.client)
    'next.jdbc (extract-namespace 'next.jdbc)
    'honeysql.core (extract-namespace 'honeysql.core)
    'honeysql.helpers (extract-namespace 'honeysql.helpers)                 
    'cheshire.core (extract-namespace 'cheshire.core)})

(defn html [v]
  (cond (vector? v)
        (let [tag (first v)
              attrs (second v)
              attrs (when (map? attrs) attrs)
              elts (if attrs (nnext v) (next v))
              tag-name (name tag)]
          (format "<%s%s>%s</%s>\n" tag-name (html attrs) (html elts) tag-name))
        (map? v)
        (str/join ""
                  (map (fn [[k v]]
                        (if (nil? v)
                          (format " %s" (name k))
                          (format " %s=\"%s\"" (name k) v))) v))
        (seq? v)
        (str/join " " (map html v))
        :else (str v)))

(def cli-options [])

(defn read-source [path]
  (try
    (str (slurp path))
    (catch java.io.FileNotFoundException fnfe nil)))

(defn format-response [status body mime-type]
  (-> (resp/response body)    
      (resp/status status)
      (resp/content-type mime-type)))   

(defn file-response [path]
  (let [mime (resp/get-mime-type (re-find #"\.[0-9A-Za-z]{1,7}$" path))]
    (-> (resp/response (io/file path))    
        (resp/status 200)
        (resp/content-type mime))))

(defn build-path [path root]
  (str root "/" path))

(defn process-includes [source parent]
  (let [includes-used (re-seq #"\(include\s*?\"(.*?)\"\s*?\)" source)]
    (loop [code source externals includes-used]
      (if (empty? externals)
        code
        (let [included (-> externals first second (build-path parent) read-source)]
          (if (nil? included)
            (throw 
              (ex-info (str "Included file '" (-> externals first second (build-path parent)) "' was not found.")
                        {:cause   (str (-> externals first first))}))
            (recur 
              (str/replace code (-> externals first first) included) 
              (rest externals))))))))
 
(defn run [url-path &{:keys [root params]}]
  (let [path (URLDecoder/decode url-path "UTF-8")
        source (read-source path)
        file (io/file path)
        parent (or root (-> file (.getParentFile) str))]
    (if (string? source)
      (let [opts  { :namespaces namespaces
                    :bindings { 'pcp (sci/new-var 'pcp params)
                                'include identity
                                'echo #(resp/response %)
                                'println println
                                'slurp #(slurp (str parent "/" %))
                                'html html
                                'response (sci/new-var 'response format-response)}
                    :classes {'org.postgresql.jdbc.PgConnection org.postgresql.jdbc.PgConnection}
                    }
            full-source (process-includes source parent)
            ans (sci/eval-string full-source opts)]
        ans)
      (format-response 404 nil nil))))

(defn file-exists? [path]
  (-> path io/file .exists))
  
(defn serve-file [root path]
  (let [full-path path
        not-found (str root "/404.clj")]
    (cond
      (file-exists? full-path) (file-response full-path)
      (file-exists? not-found) (resp/status (run not-found :root root) 404)
      :else (format-response 404 nil nil))))

(defn handle-index [root path]
  (cond
      (file-exists? (str path "index.clj"))   (run (str path "index.clj") :root root)
      (file-exists? (str path "core.clj"))    (run (str path "core.clj") :root root)
      (file-exists? (str path "main.clj"))    (run (str path "main.clj") :root root)
      (file-exists? (str path "index.html"))  (serve-file root (str path "index.html"))
      (file-exists? (str path "index.htm"))   (serve-file root (str path "index.htm"))
      :else (format-response 404 nil nil)))

(defn local-handler [request]
  (let [root admin-path
        path (str root (:uri request))]
    ;(println "HTTP:" (str/upper-case (name (:request-method request))) (:uri request))
    (cond 
      (str/ends-with? path ".clj")  (run path :root root)
      (str/ends-with? path "/")  (handle-index root path)
      :else (serve-file root path))))

(defn scgi-handler [request]
  (let [root (:document-root request)
        doc (:document-uri request)
        path (str root doc)
        r (run path :root root)
        mime (-> r :headers (get "Content-Type"))
        nl "\r\n"
        response (str (:status r) nl (str "Content-Type: " mime) nl nl (:body r))]
    ;(println "SCGI:"  (:request-method request) doc)
    response))

(defn start-servers [port admin-port]
  (println (str "SCGI server started on http://127.0.0.1:" port))
  (println (str "Admin server started on http://127.0.0.1:" admin-port))
  (future (scgi/serve port scgi-handler))
  (web/run-simpleweb local-handler {:port admin-port}))

(defn -main 
  ([]
    (-main ""))
  ([path]
    (let [port (Integer/parseInt (or (System/getenv "PORT") "9000"))
          admin-port (Integer/parseInt (or (System/getenv "ADMIN_PORT") "8000"))]
      (case path
        "" (start-servers port admin-port)
        "scgi" (start-servers port admin-port)            
        (run path)))))

      