(ns pcp.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [pcp.resp :as resp]
            [pcp.core :as core]
            [cheshire.core :as json]
            [clojure.string :as str])
  (:import  [java.io File]))

(deftest read-source-test
  (testing "Test reading source"
    (is (= "(def simple 1234)" (core/get-source "test-resources/read-source.clj")))))

(deftest read-source-not-found-test
  (testing "Test reading source when file does not exist"
    (is (str/includes? (try (core/get-source "test-resources/not-found") (catch Exception e (.getMessage e))) "o such file or directory"))))

(deftest build-path-test
  (testing "Test building path for includes"
    (is (= "/root/file/path" (core/build-path "/root" "file/path")))))

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
          scgi-request {:headers {"document-root" root} :uri uri}
          resp (core/handler scgi-request)
          ans  (core/run-script (str root uri))]
    (is (= ans resp))
    (is (= 200 (:status ans)))
    (is (= "1275" (:body ans)))
    (is (= "text/plain" (-> ans :headers (get "Content-Type")))))))

(deftest core-2-test
  (testing "Test requiring file"
    (let [root "test-resources"
          uri "/process-includes.clj"
          scgi-request {:headers {"document-root" root} :uri uri}
          _ (core/handler scgi-request)
          ans (:body (core/run-script (str root uri)))]
    (is (= (:ans ans) 5678))
    (is (true? (:working ans))))))

(deftest core-3-test
  (testing "Test requiring file that doesn't exist"
    (let [root "test-resources"
          uri "/process-includes2.clj"
          scgi-request {:headers {"document-root" root} :uri uri}
          _ (core/handler scgi-request)
          ans  (try (core/run-script (str root uri)) (catch Exception _ "error"))]
    (is (str/includes? ans "error")))))