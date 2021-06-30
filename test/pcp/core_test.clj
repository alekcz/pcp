(ns pcp.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [pcp.resp :as resp]
            [pcp.core :as core]
            [clojure.string :as str])
  (:import  [java.io File]
            [java.net Socket InetAddress ConnectException]))

(deftest read-source-test
  (testing "Test reading source"
    (is (= "(def simple 1234)" (core/read-source "test-resources/read-source.clj")))))

(deftest read-source-not-found-test
  (testing "Test reading source when file does not exist"
    (is (= nil (core/read-source "test-resources/not-found")))))

(deftest build-path-test
  (testing "Test building path for includes"
    (is (= "/root/file/path" (core/build-path "file/path" "/root" )))))

(deftest format-response-test
  (testing "Test formatting response"
    (is (resp/response? (core/format-response 202 "text" "text/plain")))))

(deftest file-response-test
  (testing "Test file response"
    (let [path "test-resources/file-response.csv"
          response (core/file-response path (io/file path))]
      (is (= 200 (:status response)))
      (is (= (io/file path) (:body response)))
      (is (= "text/csv" (-> response :headers (get "Content-Type"))))
      (is (resp/response? response)))))

(deftest file-response-404-test
  (testing "Test file response when file does not exist"
    (let [path "test-resources/not-found"
          response (core/file-response path (io/file path))]
      (is (= 404 (:status response)))
      (is (false? (.exists ^File (:body response))))
      (is (= "" (-> response :headers (get "Content-Type"))))
      (is (resp/response? response)))))     

(deftest core-test
  (testing "Test processing clj file"
    (let [root "test-resources"
          uri "/simple.clj"
          scgi-request {:document-root root :document-uri uri }
          resp (core/scgi-handler scgi-request)
          ans  (core/run-script (str root uri))]
    (is (= ans resp))
    (is (= 200 (:status ans)))
    (is (= 1275 (:body ans)))
    (is (= "text/plain" (-> ans :headers (get "Content-Type")))))))

(deftest core-2-test
  (testing "Test processing scgi request when file does not exist"
    (let [root "test-resources"
          uri "/broken.clj"
          ans  (try (core/run-script (str root uri)) (catch Exception e (.getMessage e)))]
    (is (str/includes? ans "resolve symbol")))))

(deftest core-3-test
  (testing "Test processing file directly"
    (let [root "test-resources"
          uri "/simple.clj"
          expected {:status 200, :headers {"Content-Type" "text/plain"}, :body 1275}
          ans  (core/-main (str root uri))]
    (is (= expected ans)))))    

(deftest core-4-test
  (testing "Test processing file directly that does not exist"
    (let [root "test-resources"
          uri "/non-existent"
          ans  (core/-main (str root uri))]
    (is (nil? ans)))))     

(deftest core-5-test
  (testing "Test processing slurping file that doesn't exist"
    (let [root "test-resources"
          uri "/slurp.clj"
          expected "slurp"
          ans  (core/-main (str root uri))]
    (is (= expected ans)))))  

;; (deftest core-6-test
;;   (testing "Test default connection"
;;     (let [err-connection (try (Socket. (InetAddress/getByName "127.0.0.1") 9000) (catch ConnectException _ "failed"))
;;           server (core/-main)
;;           _ (Thread/sleep 2000)
;;           socket (Socket. (InetAddress/getByName "127.0.0.1") 9000)
;;           connected (.isConnected socket)
;;           _ (do (server) (Thread/sleep 1000))]
;;     (is (= true connected))
;;     (is (= "failed" err-connection))
;;     (.close socket))))

(deftest core-7-test
  (testing "Test requiring file"
    (let [root "test-resources"
          uri "/process-includes.clj"
          scgi-request {:document-root root :document-uri uri }
          _ (core/scgi-handler scgi-request)
          ans (core/run-script (str root uri))]
    (is (= (:ans ans) 5678))
    (is (true? (:working ans))))))

(deftest core-8-test
  (testing "Test requiring file that doesn't exist"
    (let [root "test-resources"
          uri "/process-includes2.clj"
          scgi-request {:document-root root :document-uri uri }
          _ (core/scgi-handler scgi-request)
          ans  (try (core/run-script (str root uri)) (catch Exception _ "error"))]
    (is (str/includes? ans "error")))))
    
