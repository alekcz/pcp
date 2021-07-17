(ns pcp.utility
  (:require
    [pcp.resp :as resp]
    [clojure.java.io :as io]
    [clojure.edn :as edn]
    [clojure.string :as str]
    [clojure.walk :as walk]
    [clojure.java.shell :as shell]
    [org.httpkit.server :as server]
    [taoensso.nippy :as nippy]
    [environ.core :refer [env]])
  (:import  [java.net Socket]
            [java.io File ByteArrayOutputStream InputStream]
            [org.apache.commons.io IOUtils]
            [org.apache.commons.codec.digest DigestUtils]) 
  (:gen-class))

(set! *warn-on-reflection* 1)

(def root (atom nil))
(def scgi (atom "9000"))
(def version "v0.0.1")

(defn keydb []
  (or (env :pcp-keydb) "/usr/local/etc/pcp-db"))

(defn template-path []
  (or (env :pcp-template-path) "/usr/local/bin/pcp-templates"))

(defn http-to-scgi [req]
  (let [header (walk/keywordize-keys (or (:headers req) {"Content-type" "text/plain"}))
        body-len (:content-length req)
        partial  (str
                        "CONTENT_LENGTH\0" body-len "\0"
                        "REQUEST_METHOD\0" (-> req :request-method name str/upper-case) "\0"
                        "REQUEST_URI\0" (-> req :uri) "\0"
                        "QUERY_STRING\0" (-> req :query-string) "\0"
                        "CONTENT_TYPE\0" (-> req :content-type) "\0"
                        "DOCUMENT_URI\0" (-> req :document-uri) "\0"
                        "DOCUMENT_ROOT\0" (-> req :document-root) "\0"
                        "SCGI\0" 1 "\0"
                        "SERVER_PROTOCOL\0" (-> req :protocol) "\0"
                        "REQUEST_SCHEME\0" (-> req :scheme) "\0"
                        "HTTPS\0" (-> req :name) "\0"
                        "REMOTE_ADDR\0" (-> req :remote-addr) "\0"
                        "REMOTE_PORT\0" (-> req :name) "\0"
                        "SERVER_PORT\0" (-> req :server-port) "\0"
                        "SERVER_NAME\0" (-> req :server-name) "\0"
                        "HTTP_CONNECTION\0" (-> header :connection) "\0"
                        "HTTP_CACHE_CONTROL\0" (-> header :cache-control) "\0"
                        "HTTP_UPGRADE_INSECURE_REQUESTS\0" (-> header :upgrade-insecure-requests) "\0"
                        "HTTP_USER_AGENT\0" (-> header :user-agent) "\0"
                        "HTTP_SEC_FETCH_DEST\0" (-> header :sec-fetch-dest) "\0"
                        "HTTP_ACCEPT\0" (-> header :cookie) "\0"
                        "HTTP_SEC_FETCH_SITE\0" (-> header :sec-fetch-site) "\0"
                        "HTTP_SEC_FETCH_MODE\0" (-> header :sec-fetch-mode) "\0"
                        "HTTP_SEC_FETCH_USER\0" (-> header :sec-fetch-user) "\0"
                        "HTTP_ACCEPT_ENCODING\0" (-> header :accept-encoding) "\0"
                        "HTTP_ACCEPT_LANGUAGE\0" (-> header :accept-language) "\0"
                        "HTTP_COOKIE\0" (-> header :cookie) "\0")
          scgi-header (str (count partial) ":" partial ",")
          scgi-bytes (.getBytes ^String scgi-header)
          body-bytes (if (nil? (:body req)) (byte-array 0) (IOUtils/toByteArray ^InputStream (:body req)))
          baos (ByteArrayOutputStream.)]
          (.write ^ByteArrayOutputStream baos scgi-bytes 0 (count scgi-bytes))
          (.write ^ByteArrayOutputStream baos body-bytes 0 (count body-bytes))  
          (.toByteArray baos)))    

(def help 
"PCP: Clojure Processor -- Like drugs but better

Usage: pcp [option] [value]

Options:
  new [project]           Create a new pcp project in the [project] directory
  service [stop/start]    Stop/start the PCP SCGI server daemon
  passphrase [project]    Set passphrase for [project]
  secret [path]           Add and encrypt secrets at . or [path]
  -e, --evaluate [path]   Evaluate a clojure file using PCP
  -s, --serve [root]      Start a local server at . or [root]
  -v, --version           Print the version string and exit
  -h, --help              Print the command line help")

(defn safe-trim [s]
  (-> s str str/trim))

(defn forward [scgi-req scgi-port]
  (let [socket (Socket. "127.0.0.1" ^Integer scgi-port)
        os (.getOutputStream socket)
        is (.getInputStream socket)]
      (.write os scgi-req 0 (count scgi-req))
      (.flush os)
      (IOUtils/toString is)))

(defn format-response [status body mime-type]
  (-> (resp/response body)    
      (resp/status status)
      (resp/content-type mime-type))) 

(defn file-response [path ^File file]
  (let [code (if (.exists file) 200 404)
        mime (resp/get-mime-type (re-find #"\.[0-9A-Za-z]{1,7}$" path))]
    (-> (resp/response file)    
        (resp/status code)
        (resp/content-type mime))))

(defn create-resp [scgi-response]
  (let [resp-array (str/split scgi-response #"\r\n")
        resp-status (first resp-array)
        end  (+ 4 (str/index-of scgi-response "\r\n\r\n"))
        status (Integer/parseInt (if (empty? resp-status) "404" resp-status))
        body (subs scgi-response end)
        header-vals (re-seq #"(.*?): (.*?)\r\n" (subs scgi-response 0 end))
        headers (apply merge (for [h header-vals] {(nth h 1) (nth h 2)}))]
    {:status status :body body :headers headers}))

(defn file-exists? [path]
  (-> path io/file .exists))
  
(defn serve-file [path]
  (file-response path (io/file path)))

(defn local-handler [opts]
  (fn [request]
    (let [root (.getCanonicalPath (io/file (:root opts)))
          path (str root (:uri request))
          slashpath (str path "index.clj")
          exists (or (file-exists? path) (file-exists? slashpath))
          not-found (str root "/404.clj")
          full (assoc request 
                    :document-root root 
                    :document-uri (if (str/ends-with? (:uri request) "/") (str (:uri request) "index.clj") (:uri request)))]     
        (cond 
          (and (str/ends-with? (:document-uri full) ".clj") exists)
            (-> full http-to-scgi (forward (:scgi-port opts)) create-resp)
          exists 
            (serve-file path)
          (file-exists? not-found)
            (-> (assoc full :document-uri "/404.clj") http-to-scgi (forward (:scgi-port opts)) create-resp)
          :else (format-response 404 nil nil)))))

(defn run-file [path port]
  (let [path (str/replace (str "/" path) "//" "/")
        root (.getCanonicalPath (io/file "./"))
        scgi-port (Integer/parseInt (or (System/getenv "SCGI_PORT") (str port) "9000"))
        request {:document-root root :document-uri path :request-method :get}]
    (-> request http-to-scgi (forward scgi-port) create-resp :body)))

(defn clean-opts [opts]
  (apply 
    merge 
    (for [[k v] opts]
      (if (empty? (str v)) {} {k v}))))

(defn start-local-server [options] 
  (let [opts (merge 
              {:port (Integer/parseInt (or (System/getenv "PORT") "3000")) 
               :root "./" 
               :scgi-port (Integer/parseInt (or (System/getenv "SCGI_PORT") "9000"))}
              (clean-opts options))
        server (server/run-server (local-handler opts)
                {:ip "127.0.0.1" :port (:port opts) :max-body (* 100 1024 1024)})]
    (println "Targeting SCGI server on port" (:scgi-port opts))
    (println (str "Local server started on http://127.0.0.1:" (:port opts)))
    (println "Serving" (:root opts))
    server))

(def linux? 
  (-> "os.name" System/getProperty str/lower-case (str/includes? "linux")))

(defn process-service-output [output]
  (let [err (:err output)]
    (if (empty? err) "success!" (str "failed: " err))))

(defn process-query-output [output]
  (let [ans (:out output)]
    (if (or (str/includes? ans "pcp.service") (str/includes? ans "com.alekcz.pcp")) 
      "running" "stopped")))

(defn start-scgi []
  (if linux?
    (process-service-output  
      (shell/sh "systemctl" "start" "pcp.service"))
    (process-service-output  
      (shell/sh "launchctl" "load" "-w" (str (System/getProperty "user.home") "/Library/LaunchAgents/com.alekcz.pcp.plist")))))

(defn stop-scgi []
  (if linux?
    (process-service-output 
      (shell/sh "systemctl" "stop" "pcp.service"))
    (process-service-output 
      (shell/sh "launchctl" "unload" (str (System/getProperty "user.home") "/Library/LaunchAgents/com.alekcz.pcp.plist")))))

(defn query-scgi []
  (if linux?
    (process-query-output   
      (shell/sh "systemctl" "list-units" "--type=service" "--state=running"))
    (process-query-output 
      (shell/sh "launchctl" "list"))))

(defn add-secret [options]
  (let [opts (merge {:root "."} (clean-opts options))
        keypath (str (:root opts) "/pcp.edn")]
    (when-not (file-exists? keypath)
      (let [_ (println "To decrypt at runtime make sure your passphrase has been added on the server. 
                      \nPlease ensure you use the same passphrase for all your secrets in this project") 
            project-name (do (print "Project name: ") (flush) (safe-trim (read-line)))]
        (io/make-parents keypath)
        (spit keypath (prn-str {:project project-name}))))
    (let [project (-> keypath slurp edn/read-string)
          _ (do 
              (println "--------------------------------------------------")
              (println "Set an encrypted secret variable for project:" (:project project)) 
              (println "Please ensure you use the same passphrase for all your secrets in this project") 
              (println "and that you add your passphrase to your production server using:") 
              (println (str "  pcp passphrase " (:project project))) 
              (println "--------------------------------------------------"))
          env-var (do (print "Secret name: ") (flush) (safe-trim (read-line)))
          value (do (print "Secret value: ") (flush) (safe-trim (read-line)))
          password (do (print "Passphrase: ") (flush) (safe-trim (read-line)))
          path (str (:root opts) "/" ".pcp/" (-> ^String env-var ^"[B" DigestUtils/sha512Hex) ".npy")]
    (println "encrypting...")
    (io/make-parents path)
    (nippy/freeze-to-file path {:name env-var :value value} {:password [:cached password]})
    (println "done."))))

(defn add-passphrase [project]
  (let [_ (do 
            (println "--------------------------------------------------")
            (println "Set passphrase for project:" project)
            (println "This passphrase will be used for decrypting secrets at runtime.") 
            (println "--------------------------------------------------"))
        passphrase' (do (print "Passphrase: ") (flush) (read-line))
        passphrase (safe-trim passphrase')
        path (str (keydb) "/" project ".db")]
  (io/make-parents (keydb))
  (println "adding passphrase for project:" project)
  (with-open [w (io/writer path)]
    (.write w ^String passphrase))
  (println "done.")))  

(defn new-project [path]
  (let [re-filename #"(.*)\\[^\\]*"
        project-name (or (second (re-find re-filename path)) path)]
    (io/make-parents (str path "/pcp.edn"))
    (io/make-parents (str path "/public/index.clj"))
    (io/make-parents (str path "/README.md"))
    (io/make-parents (str path "/public/api/info.clj"))
    (spit (str path "/pcp.edn") (pr-str {:project project-name}))
    (spit 
      (str path "/public/index.clj") 
      (slurp (str (template-path) "/index.clj")))
    (spit 
      (str path "/README.md") 
      (slurp (str (template-path) "/README.md")))
    (spit 
      (str path "/public/api/info.clj") 
      (slurp (str (template-path) "/api/info.clj")))
    (println (str "Created pcp project `" project-name "` in directory") (.getAbsolutePath (io/file path)))))

(defn -main 
  ([]
    (-main "" ""))
  ([path]       
    (-main path ""))
  ([option value]    
    (case option
      "-s" (start-local-server {:root value})
      "--serve" (start-local-server {:root value})
      "-v" (println "pcp" version)
      "--version" (println "pcp" version)
      "-e" (println (run-file value 9000))
      "--evaluate" (println (run-file value 9000))
      "new" (new-project value)
      "passphrase" (add-passphrase value)
      "secret" (add-secret {:root value})
      "service" (case value 
                  "start" (do (println (start-scgi)) (shutdown-agents)) ;tests suites that touch this line will fail
                  "stop"  (do (println (stop-scgi)) (shutdown-agents))  ;shutdown-agents brings the house of cards 
                  "status"  (do (println (query-scgi)) (shutdown-agents))  ;crashing down.
                  (do                                                   
                    (println "unknown command:" value)
                    (println help)))
      "" (println help)
      (println help))))                               
