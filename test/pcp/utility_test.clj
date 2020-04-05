(ns pcp.utility-test
  (:require [clojure.test :refer :all]
            [pcp.scgi :as scgi]
            [pcp.core :as core]
            [pcp.resp :as resp]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [clj-http.lite.client :as client]
            [pcp.utility :as utility])
  (:import  [java.io File]
            [org.httpkit.server HttpServer]))

(deftest version-test
  (testing "Test version flags"
    (let [output (str/trim (with-out-str (utility/-main "-v")))
          output2 (str/trim (with-out-str (utility/-main "--version")))
          version (str "pcp " (slurp "resources/PCP_VERSION"))]
      (is (= version output))
      (is (= version output2)))))

(deftest format-response-test
  (testing "Test formatting response"
    (is (resp/response? (utility/format-response 202 "text" "text/plain")))))

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

(deftest server-test
  (testing "Test local server"
    (let [scgi (atom true)
          scgi-port 33333
          handler #(core/scgi-handler %)
          _ (future (scgi/serve handler scgi-port scgi))
          port 44444
          local (utility/start-local-server {:port 44444 :root "test-resources/site" :scgi-port scgi-port})]
      (let [resp-index (client/get (str "http://localhost:" port "/"))
            resp-text  (client/get (str "http://localhost:" port "/text.txt"))]
        (is (= {:name "Test" :num 1275 :end nil} (-> resp-index :body (json/decode true))))
        (is (= "12345678" (:body resp-text)))
        (is (thrown? Exception (client/get (str "http://localhost:" port "/not-there"))))
        (local)
        (reset! scgi nil)))))


(defn private-field [obj fn-name-string]
  (let [m (.. obj getClass (getDeclaredField fn-name-string))]
    (. m (setAccessible true))
    (. m (get obj))))

(deftest server-2-test
  (testing "Test local server on default port"
    (let [scgi (atom true)
          scgi-port 9000
          handler #(core/scgi-handler %)
          _ (future (scgi/serve handler scgi-port scgi))
          local (utility/-main "test-resources/site")
          resp-index (client/get (str "http://localhost:3000/"))
          resp-text  (client/get (str "http://localhost:3000/text.txt"))]
      (is (= {:name "Test" :num 1275 :end nil} (-> resp-index :body (json/decode true))))
      (is (= "12345678" (:body resp-text)))
      (is (thrown? Exception (client/get (str "http://localhost:3000/not-there"))))
      (local)
      (while (.isAlive ^Thread (private-field (:server (meta local)) "serverThread")))
      (Thread/sleep 500)
      (let [local2 (utility/-main)
            _ (Thread/sleep 1000)
            resp-index-2 (client/get (str "http://localhost:3000/test-resources/site/index.clj"))
            resp-text-2  (client/get (str "http://localhost:3000/test-resources/site/text.txt"))]
        (is (= {:name "Test" :num 1275 :end nil} (-> resp-index-2 :body (json/decode true))))
        (is (= "12345678" (:body resp-text-2)))
        (is (thrown? Exception (client/get (str "http://localhost:3000/not-there"))))
      (local2)
        (reset! scgi nil)))))