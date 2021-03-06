(ns pcp.utility-test
  (:require [clojure.test :refer :all]
            [pcp.scgi :as scgi]
            [pcp.core :as core]
            [pcp.resp :as resp]
            [pcp.utility :as utility]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [clj-http.lite.client :as client]
            [environ.core :refer [env]])
  (:import  [java.io File]))

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
          handler #(core/scgi-handler %)
          scgi (scgi/serve handler scgi-port)
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

(defn private-field [obj fn-name-string]
  (let [m (.. obj getClass (getDeclaredField fn-name-string))]
    (. m (setAccessible true))
    (. m (get obj))))

(deftest secrets-passphrase-test
  (testing "Test local server"
    (let [project "tmp-passphrase"
          root (str project "/public")
          _ (try (delete-recursively project) (catch Exception _ nil))
          _ (io/make-parents (str "./test-resources/pcp-db/" project ".db"))
          _ (utility/new-project project)
          _ (new-folder root)
          scgi-port 33333
          handler #(core/scgi-handler %)
          scgi (scgi/serve handler scgi-port)
          port 44444
          local (utility/start-local-server {:port port :root root :scgi-port scgi-port})
          env-var "SUPER_SECRET_API"
          env-var-value (rand-str 50)
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
        (is (thrown? Exception (client/get (str "http://localhost:" port "/not-there"))))
        (local)
        (scgi))))

(defn private-field [obj fn-name-string]
  (let [m (.. obj getClass (getDeclaredField fn-name-string))]
    (. m (setAccessible true))
    (. m (get obj))))

(deftest server-2-test
  (testing "Test local server on default port"
    (let [project "tmp-second"
          root (str project "/public")
          _ (try (delete-recursively project) (catch Exception _ nil))
          _ (io/make-parents (str "./test-resources/pcp-db/" project ".db"))
          _ (new-folder root)
          _ (utility/stop-scgi)
          scgi (core/-main)
          _ (Thread/sleep boot-time)
          local (utility/-main "-s" root)
          file-eval (json/decode (with-out-str (utility/-main "-e" "./test-resources/site/index.clj")) true)
          file-eval2 (json/decode (with-out-str (utility/-main "--evaluate" "./test-resources/site/index.clj")) true)
          file-eval-expected (json/decode "{\"num\":1275,\"name\":\"Test\",\"end\":null}" true)
          resp-index (client/get (str "http://localhost:3000/"))
          resp-text  (client/get (str "http://localhost:3000/text.txt"))]
      (is (= {:name "Test" :num 1275 :end nil} (-> resp-index :body (json/decode true))))
      (is (= "12345678" (:body resp-text)))
      (is (= file-eval-expected file-eval))
      (is (= file-eval-expected file-eval2))
      (is (thrown? Exception (client/get (str "http://localhost:3000/not-there"))))
      (local)
      (while (.isAlive ^Thread (private-field (:server (meta local)) "serverThread")))
      (Thread/sleep boot-time)
      (let [local2 (utility/-main "--serve" "./")
            _ (Thread/sleep boot-time)
            resp-index-2 (client/get (str "http://localhost:3000/" root "/index.clj"))
            resp-text-2  (client/get (str "http://localhost:3000/" root "/text.txt"))]
        (is (= {:name "Test" :num 1275 :end nil} (-> resp-index-2 :body (json/decode true))))
        (is (= "12345678" (:body resp-text-2)))
        (is (thrown? Exception (client/get (str "http://localhost:3000/not-there"))))
        (local2)
        (scgi)))))

(deftest keydb-test
  (testing "Test that server and utility using the same db"
    (is core/keydb utility/keydb)))

(deftest new-project
  (testing "Test that new project is created"
    (let [_ (utility/new-project "tmp")]
      (is (= (slurp "tmp/public/api/info.clj") (slurp "resources/pcp-templates/api/info.clj")))
      (is (= (slurp "tmp/README.md") (slurp "resources/pcp-templates/README.md")))
      (is (= (slurp "tmp/public/index.clj") (slurp "resources/pcp-templates/index.clj"))))))