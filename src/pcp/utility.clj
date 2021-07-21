(ns pcp.utility
  (:require
    [pcp.resp :as resp]
    [clojure.java.io :as io]
    [clojure.edn :as edn]
    [clojure.string :as str]
    [clojure.java.shell :as shell]
    [org.httpkit.server :as server]
    [taoensso.nippy :as nippy]
    [clj-http.lite.client :as http]
    ;; [clojure.tools.cli :refer [parse-opts]]
    [environ.core :refer [env]])
  (:import  [java.io File BufferedWriter]
            [org.apache.commons.codec.digest DigestUtils]) 
  (:gen-class))

(set! *warn-on-reflection* 1)

(def root (atom nil))
(def scgi (atom "9000"))
(def version "v0.0.2-beta.7")

(defn keydb []
  (or (env :pcp-keydb) "/usr/local/etc/pcp-db"))

(defn template-path []
  (or (env :pcp-template-path) "/usr/local/bin/pcp-templates")) 

(def help 
"PCP: Clojure Processor -- Like drugs but better

Usage: pcp [option] [value]

Options:
  new [project]           Create a new pcp project in the [project] directory
  service [stop/start]    Stop/start the PCP service
  passphrase [project]    Set passphrase for [project]
  secret [path]           Add and encrypt secrets at . or [path]
  -e, --evaluate [path]   Evaluate a clojure file using PCP
  -s, --serve [root]      Start a local server at . or [root]
  -v, --version           Print the version string and exit
  -h, --help              Print the command line help")

(defn safe-trim [s]
  (-> s str str/trim))

(defn forward [{:keys [request-method uri headers body]} port]
  (let [request  {:method  request-method
                  :url     (str "http://127.0.0.1:" port uri)
                  :headers (dissoc headers "content-length")
                  :body    body
                  :throw-exceptions false
                  :as      :stream}
        response (http/request request)]
    (select-keys response [:status :headers :body])))

(defn format-response [status body mime-type]
  (-> (resp/response body)    
      (resp/status status)
      (resp/content-type mime-type))) 

(defn file-response [path file]
  (let [code (if (.exists ^File file) 200 404)
        mime (resp/get-mime-type (re-find #"\.[0-9A-Za-z]{1,7}$" path))]
    (-> (resp/response file)    
        (resp/status code)
        (resp/content-type mime))))

(defn file-exists? [path]
  (-> path ^File io/file .exists))
  
(defn serve-file [path]
  (file-response path (io/file path)))

(defn local-handler [opts]
  (fn [request]
    (let [root (.getCanonicalPath ^File (io/file (:root opts)))
          path (str root (:uri request))
          slashpath (str path "/index.clj")
          exists (or (file-exists? path) (file-exists? slashpath))
          new-uri (if (str/ends-with? (:uri request) "/") (str (:uri request) "index.clj") (:uri request))
          full (-> request 
                   (assoc-in [:headers "document-root"] root)
                   (assoc :uri new-uri))]     
        (cond 
          (and (str/ends-with? (:uri full) ".clj") exists)
            (-> full (forward (:scgi-port opts)))
          exists 
            (serve-file path)
          :else (forward full (:scgi-port opts))))))

(defn run-file [path scgi-port]
  (let [path' (-> path io/file (.getCanonicalPath))
        root (-> path' io/file (.getParentFile) (.getCanonicalPath))
        final-path (-> path' (str/replace root "/") (str/replace "//" "/"))
        request {:headers {"document-root" root} :uri final-path :request-method :get :body ""}
        resp (forward request scgi-port)]
    (when (:body resp)
      (slurp (:body resp)))))

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
                {:port (:port opts) :max-body (* 100 1024 1024)})]
    (println "Targeting SCGI server on port" (:scgi-port opts))
    (println (str "Local server started on http://127.0.0.1:" (:port opts)))
    (println "Serving" (:root opts))
    (fn [] (server))))

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
    (println keypath)
    (when-not (file-exists? keypath)
      (let [_ (do (println "--------------------------------------------------")
                  (println "To decrypt at runtime make sure your passphrase has been added on the server.") 
                  (println "Please ensure you use the same passphrase for all your secrets in this project")
                  (println "--------------------------------------------------")) 
            project-name (do (print "Project name: ") (flush) (safe-trim (read-line)))
            _ (println)]
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
    (Thread/sleep 1000)
    (println "inputs:" keypath project env-var value password)
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
  (with-open [^BufferedWriter w (io/writer path)]
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
    (println (str "Created pcp project `" project-name "` in directory") (.getAbsolutePath ^File (io/file path)))))

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
      "-e" (println (run-file value (Integer/parseInt (or (System/getenv "SCGI_PORT") "9000"))))
      "--evaluate" (println (run-file value (Integer/parseInt (or (System/getenv "SCGI_PORT") "9000"))))
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
