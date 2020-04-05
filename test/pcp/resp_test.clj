(ns pcp.resp-test
  (:require [clojure.test :refer :all]
            [clj-http.lite.client]
            [pcp.resp :as resp]))

(deftest reponse-test
  (testing "FIXME, I fail."
    (let [response (resp/response "Test")]
      (is (contains? response :status))
      (is (contains? response :body))
      (is (contains? response :headers))
      (is (= 200 (:status response)))
      (is (= "Test" (:body response)))
      (is (= {} (:headers response))))))
      
(deftest status-test
  (testing "FIXME, I fail."
    (let [response (-> "Test" resp/response (resp/status 500))]
      (is (contains? response :status))
      (is (contains? response :body))
      (is (contains? response :headers))
      (is (= 500 (:status response)))
      (is (= "Test" (:body response)))
      (is (= {} (:headers response))))))

(deftest content-type-test
  (testing "FIXME, I fail."
    (let [response (-> "Test" resp/response (resp/content-type "text/plain"))]
      (is (contains? response :status))
      (is (contains? response :body))
      (is (contains? response :headers))
      (is (= 500 (:status response)))
      (is (= "Test" (:body response)))
      (is (= {} (:headers response))))))

(deftest conversions-test
  (testing "FIXME, I fail."
    (let [nums [72 101 108 108 111 32 119 111 114 108 100]
          bytes (byte-array nums)
          string "Hello world"]
      (is (= string (resp/to-string bytes)))
      (is (= nums (map int (resp/to-byte-array string))))
      (is (= string (-> string resp/to-byte-array resp/to-string))))))
