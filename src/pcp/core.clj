(ns pcp.core
  (:require
    [sci.core :as sci]
    [sci.addons :as addons]
    [pcp.resp :as resp]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [pcp.scgi :as scgi]
    [pcp.includes :refer [includes html]]
    [selmer.parser :as parser]
    [ring.middleware.keyword-params :refer [keyword-params-request]]
    [ring.middleware.defaults :refer [site-defaults wrap-defaults]])
  (:import [java.net URLDecoder]
           [java.io File]) 
  (:gen-class))


(set! *warn-on-reflection* 1)

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

(defn read-source [path]
  (try
    (str (slurp path))
    (catch java.io.FileNotFoundException fnfe nil)))

(defn build-path [path root]
  (str root "/" path))

(defn clean-source [source]
  (str/replace source #"\u003B.*\n" ""))

(defn process-includes [raw-source parent]
  (let [source (clean-source raw-source)
        includes-used (re-seq #"\(use\s*?\"(.*?)\"\s*?\)" source)]
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

(defn process-script [full-source opts]
  (sci/eval-string full-source opts))

(defn longer [str1 str2]
  (if (> (count str1) (count str2)) str1 str2))

(defn run-script [url-path &{:keys [root params]}]
  (let [path (URLDecoder/decode url-path "UTF-8")
        source (read-source path)
        file (io/file path)
        parent (longer root (-> ^File file (.getParentFile) str))]
    (if (string? source)
      (let [opts  (-> { :namespaces (merge includes {'pcp { 'params #(identity params)
                                                            'response format-response
                                                            'html html}})
                        :bindings {'println println 'use identity 'slurp #(slurp (str parent "/" %))}
                        :classes {'org.postgresql.jdbc.PgConnection org.postgresql.jdbc.PgConnection}}
                        (addons/future))
            _ (parser/set-resource-path! root)                        
            full-source (process-includes source parent)
            result (process-script full-source opts)
            _ (selmer.parser/set-resource-path! nil)]
        result)
      nil)))

(defn scgi-handler [request]
  (let [root (:document-root request)
        doc (:document-uri request)
        path (str root doc)
        r (try (run-script path :root root :params request) (catch Exception e  (format-response 500 (.getMessage e) nil)))
        mime (-> r :headers (get "Content-Type"))
        nl "\r\n"
        response (str (:status r) nl (str "Content-Type: " mime) nl nl (:body r))]
    response))

(defn -main 
  ([]
    (-main ""))
  ([path]       
    (let [scgi-port (Integer/parseInt (or (System/getenv "SCGI_PORT") "9000"))]
      (case path
        ""  (scgi/serve scgi-handler scgi-port)
        (run-script path)))))

      