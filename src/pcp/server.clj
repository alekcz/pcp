;; (ns pcp.server
;;   (:require
;;     [aleph.http.core :as http]
;;     [aleph.netty :as netty]
;;     [aleph.flow :as flow]
;;     [byte-streams :as bs]
;;     [clojure.tools.logging :as log]
;;     [clojure.string :as str]
;;     [manifold.deferred :as d]
;;     [manifold.stream :as s])
;;   (:import
;;     [java.util
;;      EnumSet
;;      TimeZone
;;      Date
;;      Locale]
;;     [java.text
;;      DateFormat
;;      SimpleDateFormat]
;;     [io.aleph.dirigiste
;;      Stats$Metric]
;;     [aleph.http.core
;;      NettyRequest]
;;     [io.netty.buffer
;;      ByteBuf]
;;     [io.netty.channel
;;      Channel
;;      ChannelHandlerContext
;;      ChannelHandler
;;      ChannelPipeline]
;;     [io.netty.handler.stream ChunkedWriteHandler]
;;     [io.netty.handler.codec.http
;;      DefaultFullHttpResponse
;;      HttpContent HttpHeaders
;;      HttpContentCompressor
;;      HttpRequest HttpResponse
;;      HttpResponseStatus DefaultHttpHeaders
;;      HttpServerCodec HttpVersion HttpMethod
;;      LastHttpContent HttpServerExpectContinueHandler]
;;     [io.netty.handler.codec.http.websocketx
;;      WebSocketServerHandshakerFactory
;;      WebSocketServerHandshaker
;;      PingWebSocketFrame
;;      PongWebSocketFrame
;;      TextWebSocketFrame
;;      BinaryWebSocketFrame
;;      CloseWebSocketFrame
;;      WebSocketFrame
;;      WebSocketFrameAggregator]
;;     [io.netty.handler.codec.http.websocketx.extensions.compression
;;      WebSocketServerCompressionHandler]
;;     [java.io
;;      IOException]
;;     [java.net
;;      InetSocketAddress]
;;     [io.netty.util.concurrent
;;      FastThreadLocal]
;;     [java.util.concurrent
;;      TimeUnit
;;      Executor
;;      ExecutorService
;;      RejectedExecutionException]
;;     [java.util.concurrent.atomic
;;      AtomicReference
;;      AtomicInteger
;;      AtomicBoolean]))

;; (set! *unchecked-math* true)

;; ;;;

;; (def ^FastThreadLocal date-format (FastThreadLocal.))
;; (def ^FastThreadLocal date-value (FastThreadLocal.))

;; (defn rfc-1123-date-string []
;;   (let [^DateFormat format
;;         (or
;;           (.get date-format)
;;           (let [format (SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:ss z" Locale/ENGLISH)]
;;             (.setTimeZone format (TimeZone/getTimeZone "GMT"))
;;             (.set date-format format)
;;             format))]
;;     (.format format (Date.))))

;; (defn ^CharSequence date-header-value [^ChannelHandlerContext ctx]
;;   (if-let [^AtomicReference ref (.get date-value)]
;;     (.get ref)
;;     (let [ref (AtomicReference. (HttpHeaders/newEntity (rfc-1123-date-string)))]
;;       (.set date-value ref)
;;       (.scheduleAtFixedRate (.executor ctx)
;;         #(.set ref (HttpHeaders/newEntity (rfc-1123-date-string)))
;;         1000
;;         1000
;;         TimeUnit/MILLISECONDS)
;;       (.get ref))))

;; (defn error-response [^Throwable e]
;;   (log/error e "error in HTTP handler")
;;   {:status 500
;;    :headers {"content-type" "text/plain"}
;;    :body (let [w (java.io.StringWriter.)]
;;            (.printStackTrace e (java.io.PrintWriter. w))
;;            (str w))})

;; (let [[server-name connection-name date-name]
;;       (map #(HttpHeaders/newEntity %) ["Server" "Connection" "Date"])

;;       [server-value keep-alive-value close-value]
;;       (map #(HttpHeaders/newEntity %) ["Aleph/0.4.4" "Keep-Alive" "Close"])]
;;   (defn send-response
;;     [^ChannelHandlerContext ctx keep-alive? ssl? rsp]
;;     (let [[^HttpResponse rsp body]
;;           (try
;;             [(http/ring-response->netty-response rsp)
;;              (get rsp :body)]
;;             (catch Throwable e
;;               (let [rsp (error-response e)]
;;                 [(http/ring-response->netty-response rsp)
;;                  (get rsp :body)])))]

;;       (netty/safe-execute ctx

;;         (doto (.headers rsp)
;;           (.set ^CharSequence server-name server-value)
;;           (.set ^CharSequence connection-name (if keep-alive? keep-alive-value close-value))
;;           (.set ^CharSequence date-name (date-header-value ctx)))

;;         (http/send-message ctx keep-alive? ssl? rsp body)))))

;; ;;;

;; (defn invalid-value-response [req x]
;;   (error-response
;;     (IllegalArgumentException.
;;       (str "cannot treat "
;;         (pr-str x)
;;         " as HTTP response for request to '"
;;         (:uri req)
;;         "'"))))

;; (defn HANDLER
;;   [^ChannelHandlerContext ctx
;;    ssl?
;;    handler
;;    rejected-handler
;;    executor
;;    ^HttpRequest req
;;    previous-response
;;    body
;;    keep-alive?]
;;    (println "req")
;;    (println "body")
;;   (let [^NettyRequest req' (http/netty-request->ring-request req ssl? (.channel ctx) body)
;;         rsp (if executor

;;               ;; handle request on a separate thread
;;               (try
;;                 (d/future-with executor
;;                   (handler req'))
;;                 (catch RejectedExecutionException e
;;                   (if rejected-handler
;;                     (try
;;                       (rejected-handler req')
;;                       (catch Throwable e
;;                         (error-response e)))
;;                     {:status 503
;;                      :headers {"content-type" "text/plain"}
;;                      :body "503 Service Unavailable"})))

;;               ;; handle it inline (hope you know what you're doing)
;;               (try
;;                 (handler req')
;;                 (catch Throwable e
;;                   (error-response e))))]

;;     (-> previous-response
;;       (d/chain'
;;         netty/wrap-future
;;         (fn [_]
;;           (netty/release req)
;;           (netty/release body)
;;           (-> rsp
;;             (d/catch' error-response)
;;             (d/chain'
;;               (fn [rsp]
;;                 (when (not (-> req' ^AtomicBoolean (.websocket?) .get))
;;                   (send-response ctx keep-alive? ssl?
;;                     (cond

;;                       (map? rsp)
;;                       rsp

;;                       (nil? rsp)
;;                       {:status 204}

;;                       :else
;;                       (invalid-value-response req rsp))))))))))))

;; (defn exception-handler [ctx ex]
;;   (when-not (instance? IOException ex)
;;     (log/warn ex "error in HTTP server")))

;; (defn invalid-request? [^HttpRequest req]
;;   (println "invalid-request" req)
;;   (-> req .decoderResult .isFailure))

;; (defn reject-invalid-request [ctx ^HttpRequest req]
;;   (println "reject-invalid-request")
;;   (d/chain
;;     (netty/write-and-flush ctx
;;       (DefaultFullHttpResponse.
;;         HttpVersion/HTTP_1_1
;;         HttpResponseStatus/REQUEST_URI_TOO_LONG
;;         (-> req .decoderResult .cause .getMessage netty/to-byte-buf)))
;;     netty/wrap-future
;;     (fn [_] (netty/close ctx))))

;; (defn ring-handler
;;   [ssl? handler rejected-handler executor buffer-capacity]
;;   (println "ring-handler")
;;   (let [buffer-capacity (long buffer-capacity)
;;         request (atom nil)
;;         buffer (atom [])
;;         buffer-size (AtomicInteger. 0)
;;         stream (atom nil)
;;         previous-response (atom nil)

;;         handle-request
;;         (fn [^ChannelHandlerContext ctx req body]
;;           (println "\n\nring-handler:handle-request" req body)
;;           (reset! previous-response
;;             (HANDLER
;;               ctx
;;               ssl?
;;               handler
;;               rejected-handler
;;               executor
;;               req
;;               @previous-response
;;               (when body (bs/to-input-stream body))
;;               false)))

;;         process-request
;;         (fn [ctx req]
;;           (println "\n\nring-handler:process-request" req)
;;           (let [s (netty/buffered-source (netty/channel ctx) #(alength ^bytes %) buffer-capacity)]
;;             (reset! stream s)
;;             (handle-request ctx req s))
;;           (reset! request req))

;;         process-last-content
;;         (fn [ctx msg]
;;           (println "\n\nring-handler:process-last-content" msg)
          
;;           (let [content (.content msg)]
;;             (if-let [s @stream]

;;               (do
;;                 (s/put! s (netty/buf->array content))
;;                 (netty/release content)
;;                 (s/close! s))

;;               (if (and (zero? (.get buffer-size))
;;                     (zero? (.readableBytes content)))

;;                 ;; there was never any body
;;                 (do
;;                   (netty/release content)
;;                   (handle-request ctx @request nil))

;;                 (let [bufs (conj @buffer content)
;;                       bytes (netty/bufs->array bufs)]
;;                   (doseq [b bufs]
;;                     (netty/release b))
;;                   (handle-request ctx @request bytes))))

;;             (.set buffer-size 0)
;;             (reset! stream nil)
;;             (reset! buffer [])
;;             (reset! request nil)))

;;         process-content
;;         (fn [ctx msg]
;;           (println "\n\nring-handler:process-content" msg)
          
;;           (let [content (.content msg)]
;;             (if-let [s @stream]

;;               ;; already have a stream going
;;               (do
;;                 (netty/put! (netty/channel ctx) s (netty/buf->array content))
;;                 (netty/release content))

;;               (let [len (.readableBytes ^ByteBuf content)]

;;                 (when-not (zero? len)
;;                   (swap! buffer conj content))

;;                 (let [size (.addAndGet buffer-size len)]

;;                   ;; buffer size exceeded, flush it as a stream
;;                   (when (< buffer-capacity size)
;;                     (let [bufs @buffer
;;                           s (doto (netty/buffered-source (netty/channel ctx) #(alength ^bytes %) buffer-capacity)
;;                               (s/put! (netty/bufs->array bufs)))]

;;                       (doseq [b bufs]
;;                         (netty/release b))

;;                       (reset! buffer [])
;;                       (reset! stream s)

;;                       (handle-request ctx @request s))))))))]

;;     (netty/channel-inbound-handler

;;       :exception-caught
;;       ([_ ctx ex]
;;         (println "exception-caught" ex))

;;       :channel-inactive
;;       ([_ ctx]
;;         (when-let [s @stream]
;;           (s/close! s))
;;         (doseq [b @buffer]
;;           (netty/release b))
;;         (.fireChannelInactive ctx))

;;       :channel-read
;;       ([_ ctx msg]
;;         (println "\n\nchannel-read" msg)
;;         (process-request ctx msg)
;;         ;; (.fireChannelRead ctx msg)
;;         ;; (cond

;;         ;;   (instance? HttpRequest msg)
;;         ;;   (if (invalid-request? msg)
;;         ;;     (reject-invalid-request ctx msg)
;;         ;;     (process-request ctx msg))

;;         ;;   (instance? HttpContent msg)
;;         ;;   (if (instance? LastHttpContent msg)
;;         ;;     (process-last-content ctx msg)
;;         ;;     (process-content ctx msg))

;;         ;;   :else
;;         ;;   (.fireChannelRead ctx msg))
;;           ))))

;; (defn pipeline-builder
;;   [handler
;;    {:keys
;;     [executor
;;      rejected-handler
;;      request-buffer-size
;;      max-initial-line-length
;;      max-header-size
;;      max-chunk-size
;;      ssl?
;;      idle-timeout]
;;     :or
;;     {request-buffer-size 16384
;;      max-initial-line-length 8192
;;      max-header-size 8192
;;      max-chunk-size 16384
;;      idle-timeout 0} :as options}]
;;   (fn [^ChannelPipeline pipeline]
;;     (println "pipeline" pipeline)
;;     (println "pipeline" pipeline)
;;     (println "options" options)
;;     (let [handler (ring-handler ssl? handler rejected-handler executor request-buffer-size)]
;;       (doto pipeline
;;         ;; (.addLast "http-server"
;;           ;; (HttpServerCodec.
;;           ;;   max-initial-line-length
;;           ;;   max-header-size
;;           ;;   max-chunk-size
;;           ;;   false))
;;         ;; (.addLast "continue-handler" (HttpServerExpectContinueHandler.))
;;         (.addLast "request-handler" ^ChannelHandler handler)
;;         (http/attach-idle-handlers idle-timeout)
;;         ))))

;; ;;;

;; (defn start-server
;;   [handler
;;    {:keys [port
;;            socket-address
;;            executor
;;            ssl-context
;;            shutdown-executor?
;;            epoll?]
;;     :or {shutdown-executor? true
;;          epoll? false}
;;     :as options}]
;;   (println "starting")
;;   (let [executor (flow/utilization-executor 0.9 512 {:metrics (EnumSet/of Stats$Metric/UTILIZATION)})]
;;     (netty/start-server
;;       (pipeline-builder
;;         handler
;;         (assoc options :executor executor :ssl? (boolean ssl-context)))
;;       ssl-context
;;       identity
;;       (when (and shutdown-executor? (instance? ExecutorService executor))
;;         #(.shutdown ^ExecutorService executor))
;;       (if socket-address socket-address (InetSocketAddress. port))
;;       epoll?)))
