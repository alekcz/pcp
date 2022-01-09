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
    [babashka.fs :as fs]
    [environ.core :refer [env]])
  (:import  [java.io File BufferedWriter]
            [org.apache.commons.codec.digest DigestUtils]) 
  (:gen-class))

(set! *warn-on-reflection* 1)

(def root (atom nil))
(def pcp-server-port (atom "9000"))
(def version "v0.0.3-alpha.2")

(defn keydb []
  (or (env :pcp-keydb) "/usr/local/etc/pcp-db"))

(defn template-path []
  (or (env :pcp-template-path) "/usr/local/pcp")) 

(def help 
"PCP: Clojure Processor -- Like drugs but better

Usage: pcp [option] [value]

Options:
  new [project]           Create a new pcp project in the [project] directory
  service [stop/start]    Stop/start the PCP service
  dev [stop/start]        Stop/start the PCP service in development mode
  passphrase [project]    Set passphrase for [project]
  secret [path]           Add and encrypt secrets at . or [path]
  serve [root]            Start a local server at . or [root]
  version                 Print the version string and exit
  help                    Print the command line help")

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
  (-> path ^File (io/file) .exists))
  
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
            (-> full (forward (:pcp-server-port opts)))
          exists 
            (serve-file path)
          :else (forward full (:pcp-server-port opts))))))

(defn run-file [path pcp-server-port]
  (let [path' (-> path ^File (io/file) (.getCanonicalPath))
        root (-> path' ^File (io/file) ^File (.getParentFile) (.getCanonicalPath))
        final-path (-> path' (str/replace root "/") (str/replace "//" "/"))
        request {:headers {"document-root" root} :uri final-path :request-method :get :body ""}
        resp (forward request pcp-server-port)]
    (when (:body resp)
      (slurp (:body resp)))))

(defn clean-opts [opts]
  (apply 
    merge 
    (for [[k v] opts]
      (if (empty? (str v)) {} {k v}))))

(defn start-local-server [options] 
  (let [opts (merge 
              {:pcp-port (Integer/parseInt (or (env :pcp-port) (env :port) "3000")) 
               :root "./" 
               :pcp-server-port (Integer/parseInt (or (env :pcp-server-port) "9000"))}
              (clean-opts options))
        server (server/run-server (local-handler opts)
                {:port (:pcp-port opts) :max-body (* 100 1024 1024)})]
    (println "Targeting PCP server on port" (:pcp-server-port opts))
    (println (str "Local server started on http://127.0.0.1:" (:pcp-port opts)))
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

(defn process-service-output-dev [output]
  (let [err (:err output)]
    (if (empty? err) "success!" (str "failed: " err))))

(defn process-query-output-dev [output]
  (let [ans (:out output)]
    (if (or (str/includes? ans "pcp-dev.service") (str/includes? ans "com.alekcz.pcp-dev")) 
      "running" "stopped")))

(defn start-scgi-dev []
  (if linux?
    (process-service-output-dev  
      (shell/sh "systemctl" "start" "pcp-dev.service"))
    (process-service-output-dev  
      (shell/sh "launchctl" "load" "-w" (str (System/getProperty "user.home") "/Library/LaunchAgents/com.alekcz.pcp-dev.plist")))))

(defn stop-scgi-dev []
  (if linux?
    (process-service-output-dev 
      (shell/sh "systemctl" "stop" "pcp-dev.service"))
    (process-service-output-dev 
      (shell/sh "launchctl" "unload" (str (System/getProperty "user.home") "/Library/LaunchAgents/com.alekcz.pcp-dev.plist")))))

(defn query-scgi-dev []
  (if linux?
    (process-query-output-dev   
      (shell/sh "systemctl" "list-units" "--type=service" "--state=running"))
    (process-query-output-dev 
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
  (if (str/blank? project)
    (println "Please specify the project name") 
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
    (println "done."))))  

(defn new-project [path]
  (let [re-filename #"(.*)\\[^\\]*"
        project-name (or (second (re-find re-filename path)) path)
        prefixed? (partial str/starts-with? path)
        target (if (not-any? prefixed? ["./" "~" "/"]) (str "./" path) path)
        finalpath (.getCanonicalPath ^File (io/file target))]
    (if-not (fs/exists? target)
      (let [res (http/get "https://github.com/alekcz/pcp-template/archive/refs/heads/master.zip" {:as :byte-array :throw-exceptions false})
            tmpdir  (System/getProperty "java.io.tmpdir")
            filename (str tmpdir "pcp-tmp/pcp.zip")
            cached (str template-path "/template.zip")]
        (cond 
          (and (= 200 (:status res)) (some? (:body res)))
          (do
            (io/make-parents filename)
            (with-open [w (io/output-stream filename)]
              (.write w ^"[B" (:body res)))
            (with-open [w (io/output-stream filename)]
              (.write w ^"[B" (:body res)))
            (fs/unzip filename target)
            (fs/copy-tree (str target "/pcp-template-master") target)
            (fs/delete-tree (str target "/pcp-template-master"))
            (spit (str target "/pcp.edn") (prn-str {:project project-name})))
          
          (fs/exists? cached)
          (do
            (fs/unzip cached target)
            (fs/copy-tree (str target "/pcp-template-master") target)
            (fs/delete-tree (str target "/pcp-template-master"))
            (spit (str target "/pcp.edn") (prn-str {:project project-name})))

          :else (println "Oops"))
        (println (str "Created pcp project " path " in directory") finalpath))
      (println "Error:" finalpath "already exists"))))

(defn -main [& args]    
  (let [option (first args)
        parameter (second args)]
    (cond
      (some #{option} '("-s" "--serve" "serve"))
      (start-local-server {:root parameter})
      
      (some #{option} '("-v" "--version" "version"))
      (println "pcp" version)
      
      (= option "new")
      (new-project parameter)
      
      (= option "passphrase")
      (add-passphrase parameter)
      
      (= option "secret")
      (add-secret {:root parameter})

      (= option "service")
      (case parameter 
        "start" (do (println (start-scgi)) (shutdown-agents)) ;tests suites that touch this line will fail
        "stop"  (do (println (stop-scgi)) (shutdown-agents))  ;shutdown-agents brings the house of cards 
        "status"  (do (println (query-scgi)) (shutdown-agents))  ;crashing down.
        (do                                                   
          (println "unknown command: service" parameter)
          (println help)))

      (= option "dev")
      (case parameter 
        "start" (do (println (start-scgi-dev)) (shutdown-agents)) ;tests suites that touch this line will fail
        "stop"  (do (println (stop-scgi-dev)) (shutdown-agents))  ;shutdown-agents brings the house of cards 
        "status"  (do (println (query-scgi-dev)) (shutdown-agents))  ;crashing down.
        (do                                                   
          (println "unknown command: service" parameter)
          (println help)))

      :else (println help))))
