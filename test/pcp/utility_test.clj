(ns pcp.utility-test
  (:require [clojure.test :refer [deftest is testing]]
            [pcp.core :as core]
            [pcp.resp :as resp]
            [pcp.utility :as utility]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [clj-http.lite.client :as client]
            [org.httpkit.client :as http]
            [environ.core :refer [env]])
  (:import  [java.io File FileInputStream]))

(def boot-time 300)

(deftest version-test
  (testing "Test version flags"
    (let [output (str/trim (with-out-str (utility/-main "-v")))
          output2 (str/trim (with-out-str (utility/-main "--version")))
          version (str "pcp " (slurp "resources/PCP_VERSION"))
          leinversion (str "pcp v" (-> "./project.clj" slurp read-string (nth 2)))]
      (is (= version output))
      (is (= version output2))
      (is (= version leinversion)))))

(deftest stop-service-test
  (testing "Stop service"
    (let [_ (utility/stop-scgi)
          status (utility/query-scgi)]
      (is (> (count status) 0)))))

(deftest start-service-test
  (testing "Start service"
    (let [_ (utility/start-scgi)
          status (utility/query-scgi)]
      (is (> (count status) 0)))))

(deftest unknown-service-test
  (testing "Start service"
    (let [unknown (with-out-str (utility/-main "service" "lala"))]
      (is (str/includes? unknown "unknown")))))     

(deftest stop-dev-service-test
  (testing "Stop service"
    (let [_ (utility/stop-scgi-dev)
          status (utility/query-scgi-dev)]
      (is (> (count status) 0)))))

(deftest start-dev-service-test
  (testing "Start service"
    (let [_ (utility/start-scgi-dev)
          status (utility/query-scgi-dev)]
      (is (> (count status) 0)))))

(deftest unknown-dev-service-test
  (testing "Start service"
    (let [unknown (with-out-str (utility/-main "dev" "lala"))]
      (is (str/includes? unknown "unknown")))))     


(deftest help-test
  (testing "Start service"
    (let [help (str/trim (with-out-str (utility/-main "-h")))
          help2 (str/trim (with-out-str (utility/-main "--help")))
          help3 (str/trim (with-out-str (utility/-main "")))
          help4 (str/trim (with-out-str (utility/-main)))]
      (is (= utility/help help))    
      (is (= utility/help help2))
      (is (= utility/help help3))
      (is (= utility/help help4)))))   

(deftest format-response-test
  (testing "Test formatting response"
    (is (resp/response? (utility/format-response 200 "text" "text/plain")))))

(deftest file-response-test
  (testing "Test file response"
    (let [path "test-resources/file-response.csv"
          response (utility/file-response path (io/file path))]
      (is (= 200 (:status response)))
      (is (= (io/file path) (:body response)))
      (is (= "text/csv" (-> response :headers (get "Content-Type"))))
      (is (resp/response? response)))))

(deftest file-response-404-test
  (testing "Test file response when file does not exist"
    (let [path "test-resources/not-found"
          response (utility/file-response path (io/file path))]
      (is (= 404 (:status response)))
      (is (false? (.exists ^File (:body response))))
      (is (= "" (-> response :headers (get "Content-Type"))))
      (is (resp/response? response)))))     

(deftest run-file-test
  (testing "Run a file"
    (let [scgi-port 22222
          handler #(core/handler %)
          scgi (core/serve handler scgi-port)
          _ (Thread/sleep boot-time)
          output (utility/run-file "test-resources/simple.clj" scgi-port)]
      (is (= "1275" output))
      (scgi))))

(defn rand-str [len]
  (apply str (take len (repeatedly #(char (+ (rand 26) 65))))))

(defn delete-recursively [fname]
  (let [func (fn [func f]
               (when (.isDirectory f)
                 (doseq [f2 (.listFiles f)]
                   (func func f2)))
               (clojure.java.io/delete-file f))]
    (func func (clojure.java.io/file fname))))

(defn new-folder [root]
  (io/make-parents (str root "/new.clj") )
  (spit (str root "/404.clj") (slurp "test-resources/site/404.clj"))
  (spit (str root "/index.clj") (slurp "test-resources/site/index.clj"))
  (spit (str root "/secret.clj") (slurp "test-resources/site/secret.clj"))
  (spit (str root "/text.txt") (slurp "test-resources/site/text.txt")))

(deftest secrets-passphrase-test
  (testing "Test secret and passphrase"
    (let [project "tmp-passphrase"
          root (str project "/public")
          _ (try (delete-recursively project) (catch Exception _ nil))
          _ (io/make-parents (str "./test-resources/pcp-db/" project ".db"))
          _ (utility/new-project project)
          _ (new-folder root)
          scgi-port 33333
          handler #(core/handler %)
          scgi (core/serve handler scgi-port)
          port 44444
          local (utility/start-local-server {:pcp-port port :root root :pcp-server-port scgi-port})
          env-var "SUPER_SECRET_API"
          env-var-value (rand-str 50)
          specify (with-out-str (utility/-main "passphrase"))
          _ (with-in-str 
              (str (env :my-passphrase) "\n") 
              (utility/-main "passphrase" project))
          _ (Thread/sleep boot-time)    
          _ (with-in-str 
              (str env-var "\n" env-var-value "\n" (env :my-passphrase) "\n") 
              (utility/-main "secret" project))
          _ (Thread/sleep boot-time)
          resp-index (client/get (str "http://localhost:" port "/"))
          resp-text  (client/get (str "http://localhost:" port "/text.txt"))
          resp-secret (client/get (str "http://localhost:" port "/secret.clj"))]
        (is (= {:name "Test" :num 1275 :end nil} (-> resp-index :body (json/decode true))))
        (is (= "12345678" (:body resp-text)))
        (is (= env-var-value (:body resp-secret)))
        (is (str/includes? specify "Please specify the project name"))
        (is (thrown? Exception (client/get (str "http://localhost:" port "/not-there"))))
        (local)
        (scgi))))

(deftest secrets-passphrase-2-test
  (testing "Test secret and passphrase when no project name available"
    (let [project "tmp-passphrase2"
          root (str project "/public")
          _ (try (delete-recursively project) (catch Exception _ nil))
          _ (io/make-parents (str "./test-resources/pcp-db/" project ".db"))
          _ (utility/new-project project)
          _ (new-folder root)
          scgi-port 23333
          handler #(core/handler %)
          scgi (core/serve handler scgi-port)
          port 24444
          local (utility/start-local-server {:pcp-port port :root root :pcp-server-port scgi-port})
          _ (clojure.java.io/delete-file (io/file (str project "/pcp.edn")))
          env-var "SUPER_SECRET_API"
          env-var-value (rand-str 50)
          _  (with-in-str 
              (str (env :my-passphrase) "\n") 
              (utility/-main "passphrase" project))
          _ (Thread/sleep boot-time)    
          _ (with-in-str 
              (str project "\n" env-var "\n" env-var-value "\n" (env :my-passphrase) "\n") 
              (utility/-main "secret" project))
          resp-secret (client/get (str "http://localhost:" port "/secret.clj"))]
        (is (= env-var-value (:body resp-secret)))
        (local)
        (scgi))))

(deftest server-2-test
  (testing "Test local server on default port"
    (let [project "tmp-second"
          root (str project "/public")
          _ (try (delete-recursively project) (catch Exception _ nil))
          _ (io/make-parents (str "./test-resources/pcp-db/" project ".db"))
          _ (new-folder root)
          _ (clojure.java.io/delete-file (str root "/404.clj"))
          _ (spit (str root "/error.clj") "(require '[asdad.sad :as fake])")
          _ (spit (str root "/500.clj") "(pcp/response :break-me :break-it :lool)")
          _ (utility/stop-scgi)
          scgi (core/-main "0")
          _ (Thread/sleep boot-time)
          local (utility/-main "-s" root)
          resp-index (client/get (str "http://localhost:3000/"))
          resp-text  (client/get (str "http://localhost:3000/text.txt"))]
      (is (= {:name "Test" :num 1275 :end nil} (-> resp-index :body (json/decode true))))
      (is (= "12345678" (:body resp-text)))
      (is (thrown? Exception (client/get (str "http://localhost:3000/not-there"))))
      (is (thrown? Exception (client/get (str "http://localhost:3000/error.clj"))))
      (local)
      (Thread/sleep (* 5 boot-time))
      (let [local2 (utility/-main "--serve" "./")
            _ (Thread/sleep boot-time)
            resp-index-2 (client/get (str "http://localhost:3000/" root "/index.clj"))
            resp-text-2  (client/get (str "http://localhost:3000/" root "/text.txt"))]
        (is (= {:name "Test" :num 1275 :end nil} (-> resp-index-2 :body (json/decode true))))
        (is (= "12345678" (:body resp-text-2)))
        (is (thrown? Exception (client/get (str "http://localhost:3000/not-there"))))
        (local2)
        (scgi)))))

(deftest server-3-test
  (testing "Test local server on default port"
    (let [project "tmp-third"
          root (str project "/public")
          _ (try (delete-recursively project) (catch Exception _ nil))
          _ (io/make-parents (str root "/echo.clj"))
          _ (io/make-parents (str root "/../tmp/keep.txt") )
          port 9998
          scgi-port 9999
          scgi (core/serve core/handler scgi-port)
          local (utility/start-local-server {:pcp-port port :root root :pcp-server-port scgi-port})
          randy (str (rand-int 100000))
          tempfile (str "../tmp/pcp-test-temp-" randy ".txt")
          _ (Thread/sleep boot-time)
          _ (spit (str root "/random.txt") randy)
          _ (spit (str root "/echo.clj") "(pcp/response 200 (-> pcp/request :params :sangoku :tempfile pcp/slurp-upload str) \"text/plain\")")
          _ (spit (str root "/temp.clj") (str "(pcp/spit \"" tempfile "\" \"123456\")" 
                                              "(pcp/response 200 (pcp/slurp \"" tempfile "\") \"text/plain\")"))
          _ (spit (str root "/404.clj") "(pcp/response 404 \"404page\" \"text/plain\")")
          _ (spit (str root "/redirect.clj") "(pcp/redirect \"/hello.clj\")")
          _ (spit (str root "/hello.clj") "(pcp/response 200 \"pew pew\" \"text/plain\")")
          _ (spit (str root "/error.clj") "(require '[asdad.sad :as fake])")
          _ (spit (str root "/500.clj") "(pcp/response 200 \"500page\" \"text/plain\")")
          _ (Thread/sleep boot-time)
          res (:body @(http/request {:url (str "http://localhost:" port "/echo.clj")
                                     :method :post
                                     :multipart [{:name "sangoku" :content (FileInputStream. (clojure.java.io/file (str root "/random.txt" ))) :filename "random.txt"}]}))
          res2 @(http/get (str "http://localhost:" port "/does-not-exist"))
          res3 @(http/get (str "http://localhost:" port "/error.clj"))
          res4 @(http/get (str "http://localhost:" port "/temp.clj"))
          res5 @(http/get (str "http://localhost:" port "/redirect.clj"))]
      (is (= randy res))
      (is (= 404 (:status res2)))
      (is (= "404page" (:body res2)))
      (is (= 500 (:status res3)))
      (is (= "500page" (:body res3)))
      (is (= "123456" (:body res4)))
      (is (= "pew pew" (:body res5)))
      (local)
      (scgi))))

(deftest keydb-test
  (testing "Test that server and utility using the same db"
    (is core/keydb utility/keydb)))

(deftest new-project
  (testing "Test that new project is created"
    (let [_ (with-out-str (utility/new-project "tmp"))
          info (:body (client/get "https://raw.githubusercontent.com/alekcz/pcp-template/master/default/public/api/info.clj" {:throw-exceptions false}))
          hello (:body (client/get "https://raw.githubusercontent.com/alekcz/pcp-template/master/default/public/hello.clj" {:throw-exceptions false}))
          readme (:body (client/get "https://raw.githubusercontent.com/alekcz/pcp-template/master/README.md" {:throw-exceptions false}))
          index (:body (client/get "https://raw.githubusercontent.com/alekcz/pcp-template/master/default/public/index.clj" {:throw-exceptions false}))
          digitalocean (:body (client/get "https://raw.githubusercontent.com/alekcz/pcp-template/master/.do/deploy.template.yaml" {:throw-exceptions false}))]
      (is (= (slurp "tmp/default/public/api/info.clj") info))
      (is (= (slurp "tmp/README.md") readme))
      (is (= (slurp "tmp/default/public/hello.clj") hello))
      (is (= (slurp "tmp/default/public/index.clj") index))
      (is (= (slurp "tmp/.do/deploy.template.yaml") digitalocean)))))
      