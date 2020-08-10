(defproject pcp "0.0.1-beta.16"
  :description "PCP: Clojure Processor - Like drugs but better"
  :url "https://github.com/alekcz/pcp"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies [ ;core
                  [org.clojure/clojure "1.9.0"]
                  [org.clojure/tools.cli "1.0.194"]
                  [borkdude/sci "0.1.1-alpha.6"]
                  [byte-streams "0.2.4"]
                  [http-kit "2.4.0"]
                  [io.replikativ/konserve "0.5.1"]
                  [org.clojars.mihaelkonjevic/konserve-pg "0.1.2"]
                  [ring "1.8.0"]
                  [cheshire "5.9.0"]
                  [danlentz/clj-uuid "0.1.9"]
                  [commons-io/commons-io "2.6"]
                  [commons-codec/commons-codec "1.14"]
                  [com.google.guava/guava "28.2-jre"]
                  [com.taoensso/nippy "2.15.0"]
                  [environ "1.1.0"]

                  ;includes for hosted environemnt
                  [selmer "1.12.19"]
                  [seancorfield/next.jdbc "1.0.409"]
                  [org.postgresql/postgresql "42.2.11"]
                  [honeysql "0.9.10"]
                  [com.draines/postal "2.0.3"]
                  [buddy "2.0.0"]
                  [tick "0.4.23-alpha"]
                  [clj-http "3.10.1"]
                  [alekcz/storyblok-clj "1.2.0"]]
  :auto-clean false
  :env {:my-passphrase "s3cr3t-p455ph4r3"}
  :plugins [[nrepl/lein-nrepl "0.3.2"]
            [lein-cloverage "1.1.2"]
            [lein-environ "1.1.0"]]
  :profiles { :scgi { :aot :all
                      :main pcp.core
                      :jar-name "useless-pcp-server.jar"
                      :uberjar-name "pcp-server.jar"}
              :utility   {  :main pcp.utility
                            :aot [pcp.utility pcp.resp]
                            :jar-name "useless-pcp.jar"
                            :uberjar-name "pcp.jar"}
              :dev {:dependencies [[org.martinklepsch/clj-http-lite "0.4.3"]]
                    :plugins [[lein-shell "0.5.0"]]}}
  :aliases
  {"pcp" ["run" "-m" "pcp.utility"]
   "scgi" ["run" "-m" "pcp.core"]
   "build-pcp" ["with-profile" "utility" "uberjar"] 
   "build-server" ["with-profile" "scgi" "uberjar"] 
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

   "run-native" ["shell" "./target/${:name} scgi"]})