(ns pcp.resp
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
    :.cljs "application/x-scittle"
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
  (get mime-types (keyword extention) ""))