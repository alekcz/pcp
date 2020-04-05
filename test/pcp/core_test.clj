(ns pcp.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [pcp.resp :as resp]
            [pcp.core :as core]
            [pcp.scgi :as scgi])
  (:import  [java.io File]) )

(deftest read-source-test
  (testing "FIXME, I fail."
    (is (= "(def simple 1234)" (core/read-source "test-resources/read-source.clj")))))

(deftest read-source-not-found-test
  (testing "FIXME, I fail."
    (is (= nil (core/read-source "test-resources/not-found")))))

(deftest build-path-test
  (testing "FIXME, I fail."
    (is (= "/root/file/path" (core/build-path "file/path" "/root" )))))

(deftest process-includes-test
  (testing "FIXME, I fail."
    (is (=  "(def test 1234)\n(def test2 5678)" 
            (core/process-includes (slurp "test-resources/process-includes.clj") "test-resources")))))

(deftest process-includes-2-test
  (testing "FIXME, I fail."
    (is (thrown? Exception 
            (core/process-includes (slurp "test-resources/process-includes2.clj") "test-resources")))))

(deftest format-response-test
  (testing "FIXME, I fail."
    (is (resp/response? (core/format-response 202 "text" "text/plain")))))

(deftest file-response-test
  (testing "FIXME, I fail."
    (let [path "test-resources/file-response.csv"
          response (core/file-response path (io/file path))]
      (is (= 200 (:status response)))
      (is (= (io/file path) (:body response)))
      (is (= "text/csv" (-> response :headers (get "Content-Type"))))
      (is (resp/response? response)))))

(deftest file-response-404-test
  (testing "FIXME, I fail."
    (let [path "test-resources/not-found"
          response (core/file-response path (io/file path))]
      (is (= 404 (:status response)))
      (is (false? (.exists ^File (:body response))))
      (is (= "" (-> response :headers (get "Content-Type"))))
      (is (resp/response? response)))))     

(deftest core-test
  (testing "FIXME, I fail."
    (let [root "test-resources"
          uri "/simple.clj"
          scgi-request {:document-root root :document-uri uri }
          resp (core/scgi-handler scgi-request)
          ans  (core/run (str root uri))]
    (is (= "200\r\nContent-Type: text/plain\r\n\r\n1275" resp))
    (is (= 200 (:status ans)))
    (is (= 1275 (:body ans)))
    (is (= "text/plain" (-> ans :headers (get "Content-Type")))))))

(deftest core-2-test
  (testing "FIXME, I fail."
    (let [root "test-resources"
          uri "/broken.clj"
          ans  (core/run (str root uri))]
    (is (= 500 (:status ans)))
    (is (= {"Content-Type" ""} (:headers ans))))))

(deftest core-3-test
  (testing "FIXME, I fail."
    (let [root "test-resources"
          uri "/simple.clj"
          expected {:status 200, :headers {"Content-Type" "text/plain"}, :body 1275}
          ans  (core/-main (str root uri))]
    (is (= expected ans)))))    

(deftest core-4-test
  (testing "FIXME, I fail."
    (let [root "test-resources"
          uri "/non-existent"
          expected {:status 200, :headers {"Content-Type" "text/plain"}, :body 1275}
          ans  (core/-main (str root uri))]
    (is (= expected ans)))))     

(deftest core-5-test
  (testing "FIXME, I fail."
    (let [root "test-resources"
          uri "/slurp.clj"
          expected "slurp"
          ans  (core/-main (str root uri))]
    (is (= expected ans)))))  