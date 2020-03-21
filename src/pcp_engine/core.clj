(ns pcp-engine.core
  (:require
   [sci.core :as sci]
   [pcp-engine.resp :as resp]
   [pcp-engine.cli :as cli]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.cli :refer [parse-opts]]
   [org.httpkit.server :as server]
   [compojure.core :refer :all]
   [compojure.route :as route]
   ;the below are included to force deps to download
   [cheshire.core :as json]
   )
  (:gen-class))


(defn extract-namespace [namespace]
  (into {} (ns-publics namespace)))

(def namespaces
  { 'cheshire.core (extract-namespace 'cheshire.core)
    ;'hiccup.core {'html (with-meta @#'hiccup/html {:sci/macro true})} I'll be bach!
    })

(def cli-options [])

(defn read-source [path]
  (try
    (str (slurp path))
    (catch java.io.FileNotFoundException fnfe nil)))

(defn format-response [status body mime-type]
  (-> (resp/response body)    
      (resp/status status)
      (resp/content-type mime-type)))   

(defn file-response [body]
    (if (resp/response? body)
      body
      (-> (resp/response body)    
          (resp/status 200))))

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

(defn run [path &{:keys [root params]}]
  (let [source (read-source path)
        file (io/file path)
        parent (-> file (.getParentFile) str)]
    (if (string? source)
      (let [opts  { :namespaces namespaces
                    :bindings { 'pcp (sci/new-var 'pcp params)
                                'include identity
                                'echo #(resp/response %)
                                'println println
                                'response (sci/new-var 'response format-response)}}
              full-source (process-includes source parent)]
          (sci/eval-string full-source opts))
      (format-response 404 nil nil))))

(defn file-exists? [path]
  (-> path io/file .exists))
  
(defn serve-file [root path]
  (let [full-path (str root path)
        not-found (str root "/404.clj")]
    (cond
      (file-exists? full-path) (io/file full-path)
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

(defn handler [root request]
  (let [path (str root (:uri request))]
    (cond 
      (str/ends-with? path ".clj")  (run path :root root)
      (str/ends-with? path "/")  (handle-index root path)
      :else (serve-file root path))))

(defn make-routes [root]
  (routes
    (ANY "*" [] (fn [request] 
                  (let [resp (handler root request)] 
                    (println (str (-> request :request-method name str/upper-case) " " (:uri request) " " (:status resp)))
                    resp)))))

(defn start-server [root-dir]
  (let [root (-> root-dir (or "./src") io/file .getCanonicalPath)
        port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
    (server/run-server (make-routes root-dir) {:port port})
    (println (str "Serving " root " at http://127.0.0.1:" port))))

(defn -main [path & args]
  (let [opts (parse-opts args cli-options)
        param (first (:arguments opts))]
    (case path
      "login" (cli/login param)
      "logout" (cli/logout)
      "list"  (cli/list-sites)
      "deploy"  (cli/deploy-site param)
      "serve"  (start-server param)
      (run path))))