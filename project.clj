(defproject pcp "0.0.3-alpha"
  :description "PCP: Clojure Processor - A Clojure replacement for PHP"
  :url "https://github.com/alekcz/pcp"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}

  :dependencies [ ;core
                  [org.clojure/clojure "1.10.3"]
                  [org.clojure/tools.cli "1.0.194"]
                  [org.clojure/core.async "1.3.618"]
                  [borkdude/sci "0.2.5"]
                  [byte-streams "0.2.4"]
                  [http-kit "2.5.3"]
                  [ring "1.9.3"]
                  [cheshire "5.9.0"]
                  [danlentz/clj-uuid "0.1.9"]
                  [commons-io/commons-io "2.6"]
                  [commons-codec/commons-codec "1.14"]
                  [com.google.guava/guava "28.2-jre"]
                  [com.taoensso/nippy "3.1.1"]
                  [environ "1.1.0"]
                  [hiccup "2.0.0-alpha2"]
                  [io.replikativ/hasch "0.3.7"]
                  [org.clojure/core.cache "1.0.207"]
                  [org.martinklepsch/clj-http-lite "0.4.3"]
                  
                  ;includes for hosted environemnt
                  [selmer "1.12.19"]
                  [seancorfield/next.jdbc "1.1.582"]
                  [org.postgresql/postgresql "42.2.11"]
                  [honeysql "0.9.10"]
                  [com.draines/postal "2.0.3"]
                  [buddy "2.0.0"]
                  [tick "0.4.23-alpha"]
                  [alekcz/storyblok-clj "1.2.0"]
                  [garden "1.3.10"]
                  [io.replikativ/konserve "0.6.0-alpha3" 
                      :exclusions  [org.clojure/clojure 
                                    org.clojure/clojurescript]]
                  [alekcz/konserve-jdbc "0.1.0-20210521.205028-12" 
                    :exclusions  [org.clojure/clojure
                                  org.clojure/clojurescript
                                  com.h2database/h2 
                                  org.apache.derby/derby
                                  com.microsoft.sqlserver/mssql-jdbc]]]
  :main ^:skit-aot pcp.core
  :auto-clean false
  :dev-dependencies [[eftest/eftest "0.5.9"]]
  :plugins [[nrepl/lein-nrepl "0.3.2"]
            [lein-cloverage "1.2.2"]
            [lein-environ "1.1.0"]
            [lein-eftest "0.5.9"]]
  :cloverage {:runner :eftest
              :runner-opts {:test-warn-time 500
                           :fail-fast? false
                           :multithread? :vars}}
  :profiles { :pcp-server {:aot :all
                      :main pcp.core
                      :jar-name "useless-pcp-server.jar"
                      :uberjar-name "pcp-server.jar"
                      :strict "0"}
              :utility   {  :main pcp.utility
                            :aot [pcp.utility pcp.resp]
                            :jar-name "useless-pcp.jar"
                            :uberjar-name "pcp.jar"}
              :test {:env {:my-passphrase "s3cr3t-p455ph4r3"
                           :strict "0"
                           :pcp-template-path "resources/pcp-templates"}}
              :dev {:dependencies [[eftest/eftest "0.5.9"]
                                   [org.slf4j/slf4j-simple "1.7.32"]]
                    :plugins [[lein-shell "0.5.0"]]
                    :env {:my-passphrase "s3cr3t-p455ph4r3"
                          :strict "0"}}}
  :aliases
  {"pcp" ["run" "-m" "pcp.utility"]
   "pcp-server" ["run" "-m" "pcp.core"]
   "build-pcp" ["with-profile" "utility" "uberjar"] 
   "build-server" ["with-profile" "pcp-server" "uberjar"] 
   "native"
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
    "-H:ReflectionConfigurationFiles=resources/reflect-config.json"
    "-jar" "./target/${:name}.jar"
    "-H:Name=./target/${:name}"]

   "run-native" ["shell" "./target/${:name} pcp-server"]})