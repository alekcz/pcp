(defproject pcp-engine "0.1.0-SNAPSHOT"
  :description "PCP: Clojure Processor - Like drugs but better"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [ [org.clojure/clojure "1.10.1"]
                  [org.clojure/tools.cli "1.0.194"]
                  [http-kit "2.3.0"]
                  [borkdude/sci "0.0.13-alpha.12"]
                  [cheshire "5.9.0"]
                ]
  :main ^:skip-aot pcp-engine.core
  :target-path "target/%s"
  :profiles { :uberjar {:aot :all
                        :uberjar-name "pcp-engine.jar"}
              :dev  {:plugins [[lein-shell "0.5.0"]]}}
  ;https://github.com/BrunoBonacci/graalvm-clojure/blob/master/doc/clojure-graalvm-native-binary.md
  :aliases
  {"native"
  ["shell"
    "native-image" "--report-unsupported-elements-at-runtime"
    "--initialize-at-build-time"
    "--no-server"
    "--no-fallback"
    ;"-jar" "./target/uberjar/${:uberjar-name:-${:name}-${:version}.jar}"
    "-jar" "./target/uberjar/${:uberjar-name:-${:name}.jar}"
    "-H:Name=./target/pcp"]})
