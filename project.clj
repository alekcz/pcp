(defproject pcp-engine "0.1.0-SNAPSHOT"
  :description "PCP: Clojure Processor - Like drugs but better"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}

  :managed-dependencies
  [[io.netty/netty-codec-http "4.1.39.Final"]
   [io.netty/netty-codec "4.1.39.Final"]
   [io.netty/netty-handler-proxy "4.1.39.Final"]
   [io.netty/netty-codec-socks "4.1.39.Final"]
   [io.netty/netty-handler "4.1.39.Final"]
   [io.netty/netty-resolver-dns "4.1.39.Final"]
   [io.netty/netty-codec-dns "4.1.39.Final"]
   [io.netty/netty-resolver "4.1.39.Final"]
   [io.netty/netty-transport-native-epoll "4.1.39.Final"]
   [io.netty/netty-common "4.1.39.Final"]
   [io.netty/netty-transport-native-unix-common "4.1.39.Final"]
   [io.netty/netty-transport "4.1.39.Final"]
   [io.netty/netty-buffer "4.1.39.Final"]]

  :dependencies [ [org.clojure/clojure "1.9.0"]
                  [org.clojure/tools.cli "1.0.194"]
                  [org.martinklepsch/clj-http-lite "0.4.3"]
                  [compojure "1.6.1"]
                  [borkdude/sci "0.0.13-alpha.12"]
                  [cheshire "5.9.0"]
                  [http-kit "2.3.0"] 
                ]
  :main pcp-engine.core
  ;:main ^:skip-aot pcp-engine.core
  :plugins [[io.taylorwood/lein-native-image "0.3.0"]
            [nrepl/lein-nrepl "0.3.2"]]

  :native-image {:name     "pcp"
                 :jvm-opts ["-Dclojure.compiler.direct-linking=true"]
                 :opts     ["--enable-url-protocols=http"
                            "--report-unsupported-elements-at-runtime"
                            "--no-fallback"
                            "--initialize-at-build-time"
                            "--allow-incomplete-classpath"
                            ;"--initialize-at-run-time=io.netty.channel.epoll.EpollEventArray,io.netty.channel.unix.Errors,io.netty.channel.unix.IovArray,io.netty.channel.unix.Socket,io.netty.channel.epoll.Native,io.netty.channel.epoll.EpollEventLoop,io.netty.util.internal.logging.Log4JLogger"
                            ;"-H:+TraceClassInitialization" 
                            ;"-H:+ReportExceptionStackTraces"
                            ;;avoid spawning build server
                            "--no-server"]})

  ; ;https://github.com/BrunoBonacci/graalvm-clojure/blob/master/doc/clojure-graalvm-native-binary.md
  ; :aliases
  ; {"native"
  ; ["shell"
  ;   "native-image" "--report-unsupported-elements-at-runtime"
  ;   "--initialize-at-build-time"
  ;   "--no-server"
  ;   "--no-fallback"
  ;   ;"-jar" "./target/uberjar/${:uberjar-name:-${:name}-${:version}.jar}"
  ;   "-jar" "./target/uberjar/${:uberjar-name:-${:name}.jar}"
  ;   "-H:Name=./target/pcp"]})
