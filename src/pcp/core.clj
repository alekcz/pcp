(ns pcp.core
  (:require
    [sci.core :as sci ] 
    [sci.addons.future :as future]
    [pcp.resp :as resp]
    [clojure.java.io :as io]
    [clojure.edn :as edn]
    [clojure.string :as str]
    ;; [pohjavirta.server :as pohjavirta]
    [org.httpkit.server :as server]
    [pcp.includes :refer [includes]]
    [hiccup.compiler :as compiler]
    [hiccup.util :as util]       
    [selmer.parser :as parser]
    [clojure.walk :as walk]
    [taoensso.nippy :as nippy]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.multipart-params :refer [wrap-multipart-params]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [environ.core :refer [env]]
    [hasch.core :as h]
    [clojure.core.cache.wrapped :as c])
  (:import [java.net URLDecoder]
           [java.io File]
           [org.apache.commons.codec.digest DigestUtils]) 
  (:gen-class))

(set! *warn-on-reflection* 1)

(def cache (c/ttl-cache-factory {} :ttl (* 30 60 1000)))
(def source-cache (c/lru-cache-factory {} :threshold 1024))

(defn keydb []
  (or (env :pcp-keydb) "/usr/local/etc/pcp-db"))

(defn render-html [& contents]
  (binding [util/*escape-strings?* true]
    (apply compiler/render-html contents)))

(defn render-html-unescaped [& contents]
  (binding [util/*escape-strings?* false]
    (apply compiler/render-html contents)))

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

(defn ->keyword [any] 
  (keyword (str (h/uuid any))))

(defn read-source [path]
  {:modified (.lastModified (io/file path))
   :contents (slurp path)})

(defn get-source [path]
  (let [sk (->keyword path)
        last-modified (.lastModified (io/file path))
        cached-source (c/lookup source-cache sk)
        s (when (or (nil? cached-source) 
                    (> last-modified (:modified cached-source))) 
                (read-source path))
        source  (if (nil? s) 
                  cached-source
                  (do 
                    (c/through-cache source-cache sk (constantly s))
                    s) )]
    (:contents source)))

(defn build-path [root path]
  (str root "/" path))

(defn include [parent {:keys [namespace]}]
  (try
    {:file namespace
     :source (get-source (str parent "/" (-> namespace (str/replace "." "/") (str/replace "-" "_")) ".clj"))}
    (catch java.io.FileNotFoundException _ nil)))
     
(defn process-script [full-source opts]
  (sci/eval-string full-source opts)) ;sci/eval-string* pass context

(defn longer [str1 str2]
  (if (> (count str1) (count str2)) str1 str2))

(defn get-secret [root env-var]
  (try
    (let [project (-> (str root "/../pcp.edn") slurp edn/read-string :project)
          keypath (str (keydb) "/" project ".db")
          secret (nippy/thaw-from-file 
                  (str root "/../.pcp/"  
                    (-> ^String env-var str/trim ^"[B" DigestUtils/sha512Hex) ".npy") 
                    {:password [:cached (-> keypath slurp)]})]
      (if (= env-var (:name secret)) 
        (:value secret)
        nil))
    (catch java.io.FileNotFoundException _ (println "No passphrase has been set for this project") nil)
    (catch Exception _ nil)))

(defn valid-path? [parent target]
  (when target
    (let [parent-path (-> parent ^File io/as-file (.getParentFile) (.getCanonicalPath))
          target-path (-> target ^File io/as-file (.getCanonicalPath))]
      (str/starts-with? target-path parent-path))))

(defn temporary? [^File target]
  (when target
    (let [target-path (.getAbsolutePath target)
          temp-path (System/getProperty "java.io.tmpdir")]
      (str/starts-with? target-path temp-path))))

(def persist ^:sci/macro
  (fn [_&form _&env k f & args]
    `(let [r ($/retrieve ~k)]
        (if (some? r)
            r
            (let [s (apply ~f ~args)]
              ($/persist! ~k s)
              s)))))

(defn run-script [url-path &{:keys [root request status]}]
  (let [status (atom status)
        path' (URLDecoder/decode url-path "UTF-8")
        path  (if (-> path' (io/file) (.exists))
                  path' 
                  (when (-> (str root "/404.clj") (io/file) (.exists))
                    (reset! status 404)
                    (str root "/404.clj")))]
    (if (nil? path)   
      (format-response 404 "" nil)
      (let [^File file (io/file path)
            source (get-source path)
            root (or root (-> ^File file (.getParentFile) (.getCanonicalPath)))
            parent (longer root (-> ^File file (.getParentFile) (.getCanonicalPath)))
            response (atom nil)
            keygen (fn [path k] (keyword (str (h/uuid [path k]))))]     
        (if (string? source)
          (let [opts  (-> { :load-fn #(include root %)
                            :namespaces (merge includes { '$   {'persist! (fn [k v] (k (c/through-cache cache (keygen root k) (constantly v))))
                                                                'retrieve (fn [k]   (c/lookup cache (keygen root k)))}
                                                          'pcp {'persist persist
                                                                'clear   (fn [k]   (c/evict cache (keygen root k)))
                                                                'request request
                                                                'response (fn [status body mime-type] (reset! response (format-response status body mime-type)))
                                                                'html render-html
                                                                'render-html render-html
                                                                'html-unescaped render-html-unescaped
                                                                'render-html-unescaped render-html-unescaped
                                                                'secret #(when root (get-secret root %))
                                                                'now #(System/currentTimeMillis)
                                                                'slurp-upload (fn [^File f] (when (temporary? f) (slurp f)))
                                                                'slurp (fn [f] (when (valid-path? root (build-path parent f))  (slurp (build-path parent f))))
                                                                'spit  (fn [f content] (when (valid-path? root (build-path parent f))  (spit (build-path parent f) content)))}})
                            :bindings {}
                            :classes {'org.postgresql.jdbc.PgConnection org.postgresql.jdbc.PgConnection}}
                            (future/install))
                _ (parser/set-resource-path! root)                        
                result (process-script source opts)
                _ (selmer.parser/set-resource-path! nil)
                final-result (if (nil? @response) result @response)]
            (if @status (assoc final-result :status @status) final-result))
          (format-response 404 "" nil))))))

(defn e500 [root req message]
  (let [uri "/500.clj"
        error-page  (str root uri)]
    (if (-> error-page (io/file) (.exists))
        (try 
          (run-script error-page :root root :request (assoc req :error message :uri uri) :status 500) 
          (catch Exception e (format-response 500 (.getMessage e) nil)))
        (format-response 500 message nil))))

(defn actual-handler [request]
  (let [headers (-> request :headers walk/keywordize-keys)
        root (:document-root headers)
        doc (:uri request)
        path (str root doc)
        r (try (run-script path :root root :request request) (catch Exception e (e500 root request (.getMessage e))))]
    r))

(def handler 
   (-> #'actual-handler 
        wrap-keyword-params
        wrap-params 
        wrap-multipart-params ))

;; (defn pohjavirta [handler port]
;;   (let [s (pohjavirta/create handler {:port port})]
;;     (pohjavirta/start s)
;;     (fn [] 
;;       (pohjavirta/stop s))))

(defn serve [handler port]
  (let [s (server/run-server handler {:port port})]
    (fn [] 
      (s))))

(defn -main []
  (let [scgi-port (Integer/parseInt (or (System/getenv "SCGI_PORT") "9000"))]
    (serve handler scgi-port)))

      