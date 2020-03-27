(defproject pcp "0.1.0-SNAPSHOT"
  :description "PCP: Clojure Processor - Like drugs but better"
  :url "https://github.com/alekcz/pcp"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies [ [org.clojure/clojure "1.9.0"]
                  [org.clojure/tools.cli "1.0.194"]
                  [org.martinklepsch/clj-http-lite "0.4.3"]
                  [compojure "1.6.1"]
                  [borkdude/sci "0.0.13-alpha.12"]
                  [http-kit "2.3.0"]
                  ;includes for hosted environemnt
                  [cheshire "5.9.0"]
                  [de.ubercode.clostache/clostache "1.4.0"]
                  [hiccup "1.0.5"]
                  [seancorfield/next.jdbc "1.0.409"]
                  [org.postgresql/postgresql "42.2.11"]
                  [honeysql "0.9.10"]
                  [org.martinklepsch/clj-http-lite "0.4.3"]
                ]
  :main pcp.core
  :plugins [[io.taylorwood/lein-native-image "0.3.0"]
            [nrepl/lein-nrepl "0.3.2"]]

  :native-image {:name     "pcp"
                 :jvm-opts ["-Dclojure.compiler.direct-linking=true"]
                 :opts     ["--enable-url-protocols=http,https"
                            "--report-unsupported-elements-at-runtime"
                            "--no-fallback"
                            "--initialize-at-build-time"
                            "--allow-incomplete-classpath"
                            "--initialize-at-run-time=org.postgresql.sspi.SSPIClient"
                            "--enable-all-security-services"
                            "--no-server"
                            "-H:ReflectionConfigurationFiles=reflect-config.json"]})


