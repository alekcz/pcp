(defproject pcp "0.0.1-SNAPSHOT"
  :description "PCP: Clojure Processor - Like drugs but better"
  :url "https://github.com/alekcz/pcp"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies [ ;core
                  [org.clojure/clojure "1.9.0"]
                  [org.clojure/tools.cli "1.0.194"]
                  [borkdude/sci "0.0.13-alpha.12"]
                  [ring-simpleweb-adapter "0.2.0"]
                  [org.simpleframework/simple "4.1.21"]
                  [io.replikativ/konserve "0.5.1"]
                  ;optimizing
                  ;[com.climate/claypoole "1.1.4"]
                  
                  ;includes for hosted environemnt
                  [cheshire "5.9.0"]
                  [de.ubercode.clostache/clostache "1.4.0"]
                  [org.martinklepsch/clj-http-lite "0.4.3"]
                  [seancorfield/next.jdbc "1.0.409"]
                  [org.postgresql/postgresql "42.2.11"]
                  [honeysql "0.9.10"]
                  ;[com.draines/postal "2.0.3"]
                ]
  :main pcp.core
  :plugins [[io.taylorwood/lein-native-image "0.3.0"]
            [nrepl/lein-nrepl "0.3.2"]]
  :profiles {:uberjar {:aot :all}
            :dev {:plugins [[lein-shell "0.5.0"]]}}
  :aliases
  {"native"
   ["shell"
    "native-image" 
    "--enable-url-protocols=http,https"
    "--report-unsupported-elements-at-runtime"
    "--no-fallback"
    "--initialize-at-build-time"
    "--allow-incomplete-classpath"
    "--initialize-at-run-time=org.postgresql.sspi.SSPIClient"
    "--enable-all-security-services"
    "--no-server"
    "-H:ReflectionConfigurationFiles=reflect-config.json"
    "-jar" "./target/${:uberjar-name:-${:name}-${:version}-standalone.jar}"
    "-H:Name=./target/${:name}"]

   "run-native" ["shell" "./target/${:name} scgi"]})

  ; :native-image {:name     "pcp"
  ;                :jvm-opts ["-Dclojure.compiler.direct-linking=true"]
  ;                :opts     ["--enable-url-protocols=http,https"
  ;                           "--report-unsupported-elements-at-runtime"
  ;                           "--no-fallback"
  ;                           "--initialize-at-build-time"
  ;                           "--allow-incomplete-classpath"
  ;                           "--initialize-at-run-time=org.postgresql.sspi.SSPIClient"
  ;                           "--enable-all-security-services"
  ;                           "--no-server"
  ;                           "-H:ReflectionConfigurationFiles=reflect-config.json"]})


