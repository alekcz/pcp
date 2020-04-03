(ns pcp.resp
  (:require [clojure.walk :as walk]
            [clojure.string :as str])
  (:gen-class))

(set! *warn-on-reflection* 1)

(defn response
  "Returns a skeletal Ring response with the given body, status of 200, and no
  headers."
  [body]
  {:status  200
   :headers {}
   :body    body})

(defn status
  "Returns an updated Ring response with the given status."
  [resp status]
  (assoc resp :status status))

(defn header
  "Returns an updated Ring response with the specified header added."
  [resp name value]
  (assoc-in resp [:headers name] (str value)))

(defn content-type
  "Returns an updated Ring response with the a Content-Type header corresponding
  to the given content-type."
  [resp content-type]
  (header resp "Content-Type" content-type))

(defn response?
  "True if the supplied value is a valid response map."
  [resp]
  (and (map? resp)
       (integer? (:status resp))
       (map? (:headers resp))))

(defn to-byte-array [text]
  (.getBytes ^String text "UTF-8"))

(defn to-string [bytes]
  (String. ^"[B" bytes "UTF-8"))       

(def mime-types
  {
    :.aac "audio/aac"
    :.abw "application/x-abiword"
    :.arc "application/x-freearc"
    :.avi "video/x-msvideo"
    :.azw "application/vnd.amazon.ebook"
    :.bin "application/octet-stream"
    :.bmp "image/bmp"
    :.bz "application/x-bzip"
    :.bz2 "application/x-bzip2"
    :.csh "application/x-csh"
    :.css "text/css"
    :.csv "text/csv"
    :.doc "application/msword"
    :.docx "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    :.eot "application/vnd.ms-fontobject"
    :.epub "application/epub+zip"
    :.gz "application/gzip"
    :.gif "image/gif"
    :.htm "text/html"
    :.html "text/html"
    :.ico "image/vnd.microsoft.icon"
    :.ics "text/calendar"
    :.jar "application/java-archive"
    :.jpeg "image/jpeg"
    :.jpg "image/jpeg"
    :.js "text/javascript"
    :.json "application/json"
    :.jsonld "application/ld+json"
    :.mid "audio/midi"
    :.midi "audio/midi"
    :.mjs "text/javascript"
    :.mp3 "audio/mpeg"
    :.mpeg "video/mpeg"
    :.mpkg "application/vnd.apple.installer+xml"
    :.odp "application/vnd.oasis.opendocument.presentation"
    :.ods "application/vnd.oasis.opendocument.spreadsheet"
    :.odt "application/vnd.oasis.opendocument.text"
    :.oga "audio/ogg"
    :.ogv "video/ogg"
    :.ogx "application/ogg"
    :.opus "audio/opus"
    :.otf "font/otf"
    :.png "image/png"
    :.pdf "application/pdf"
    :.php "application/php"
    :.ppt "application/vnd.ms-powerpoint"
    :.pptx "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    :.rar "application/vnd.rar"
    :.rtf "application/rtf"
    :.sh "application/x-sh"
    :.svg "image/svg+xml"
    :.swf "application/x-shockwave-flash"
    :.tar "application/x-tar"
    :.tif "image/tiff"
    :.tiff "image/tiff"
    :.ts "video/mp2t"
    :.ttf "font/ttf"
    :.txt "text/plain"
    :.vsd "application/vnd.visio"
    :.wav "audio/wav"
    :.weba "audio/webm"
    :.webm "video/webm"
    :.webp "image/webp"
    :.woff "font/woff"
    :.woff2 "font/woff2"
    :.xhtml "application/xhtml+xml"
    :.xls "application/vnd.ms-excel"
    :.xlsx "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    :.xml "application/xml"
    :.xul "application/vnd.mozilla.xul+xml"
    :.zip "application/zip"
    :.3gp "video/3gpp"
    :.3g2 "video/3gpp2"
    :.7z "application/x-7z-compressed"})

(defn get-mime-type [extention]
  (get mime-types (keyword extention)))

(defn http-to-scgi [req]
  (let [header (walk/keywordize-keys (:headers req))]
    (str
      "REQUEST_METHOD\0" (-> req :request-method name str/upper-case)  "\n"
      "REQUEST_URI\0" (-> req :uri) "\n"
      "QUERY_STRING\0" (-> req :query-string) "\n"
      "CONTENT_TYPE\0" (-> req :content-type) "\n"
      "DOCUMENT_URI\0" (-> req :document-uri) "\n"
      "DOCUMENT_ROOT\0" (-> req :document-root) "\n"
      "SCGI\0" 1 "\n"
      "SERVER_PROTOCOL\0" (-> req :protocol) "\n"
      "REQUEST_SCHEME\0" (-> req :scheme) "\n"
      "HTTPS\0" (-> req :name) "\n"
      "REMOTE_ADDR\0" (-> req :remote-addr) "\n"
      "REMOTE_PORT\0" (-> req :name) "\n"
      "SERVER_PORT\0" (-> req :server-port) "\n"
      "SERVER_NAME\0" (-> req :server-name) "\n"
      "HTTP_CONNECTION\0" (-> header :connection) "\n"
      "HTTP_CACHE_CONTROL\0" (-> header :cache-control) "\n"
      "HTTP_UPGRADE_INSECURE_REQUESTS\0" (-> header :upgrade-insecure-requests) "\n"
      "HTTP_USER_AGENT\0" (-> header :user-agent) "\n"
      "HTTP_SEC_FETCH_DEST\0" (-> header :sec-fetch-dest) "\n"
      "HTTP_ACCEPT\0" (-> header :cookie) "\n"
      "HTTP_SEC_FETCH_SITE\0" (-> header :sec-fetch-site) "\n"
      "HTTP_SEC_FETCH_MODE\0" (-> header :sec-fetch-mode) "\n"
      "HTTP_SEC_FETCH_USER\0" (-> header :sec-fetch-user) "\n"
      "HTTP_ACCEPT_ENCODING\0" (-> header :accept-encoding) "\n"
      "HTTP_ACCEPT_LANGUAGE\0" (-> header :accept-language) "\n"
      "HTTP_COOKIE\0" (-> header :cookie) "\n"
      "\n,")))