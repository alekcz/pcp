(ns pcp.utility-test
  (:require [clojure.test :refer :all]
            [pcp.scgi :as scgi]
            [pcp.core :as core]
            [pcp.resp :as resp]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [clj-http.lite.client :as client]
            [pcp.utility :as utility]))

(deftest version-test
  (testing "FIXME, I fail."
    (let [output (str/trim (with-out-str (utility/-main "-v")))
          output2 (str/trim (with-out-str (utility/-main "-v")))
          version (str "pcp " (slurp "resources/PCP_VERSION"))]
      (is (= version output))
      (is (= version output2)))))

(deftest format-response-test
  (testing "FIXME, I fail."
    (is (resp/response? (utility/format-response 202 "text" "text/plain")))))

(deftest file-response-test
  (testing "FIXME, I fail."
    (let [path "test-resources/file-response.csv"
          response (utility/file-response path (io/file path))]
      (is (= 200 (:status response)))
      (is (= (io/file path) (:body response)))
      (is (= "text/csv" (-> response :headers (get "Content-Type"))))
      (is (resp/response? response)))))


(deftest server-test
  (testing "FIXME, I fail."
    (let [scgi (atom true)
          local (atom nil)
          handler #(core/scgi-handler %)
          _ (future (scgi/serve handler 9000 scgi))
          port 44444
          _ (reset! local (utility/start-local-server 44444 "test-resources/site"))]
      (Thread/sleep 1000)
      (let [resp-index (client/get (str "http://localhost:" port "/"))
            resp-text  (client/get (str "http://localhost:" port "/text.txt"))]
        (is (= {:name "Test" :num 1275 :end nil} (-> resp-index :body (json/decode true))))
        (is (= "12345678" (:body resp-text)))
        (reset! local nil)
        (reset! scgi nil)))))