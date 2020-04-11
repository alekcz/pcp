(ns pcp.scgi-test
  (:require [clojure.test :refer :all]
            [pcp.scgi :as scgi]
            [pcp.core :as core]
            [clojure.java.io :as io])
  (:import  [java.net Socket InetAddress]
            [java.io Writer]
            [org.apache.commons.io IOUtils]))

(deftest serve-test
  (testing "Test SCGI server"
    (let [running (atom true)
          handler #(core/scgi-handler %)
          scgi-port 55555
          _ (future (scgi/serve handler scgi-port running))
          message (IOUtils/toByteArray (io/input-stream "test-resources/scgi.bin"))
          len (count message)]
      (Thread/sleep 500)
      (let [socket (Socket. (InetAddress/getByName "127.0.0.1") scgi-port)]
        (with-open [os (io/output-stream (.getOutputStream socket))]
          (.write os message 0 len)
          (.flush os)
          (let [ans (IOUtils/toString (.getInputStream socket))
                _ (.close socket)]
            (is (= "200\r\nContent-Type: text/plain\r\n\r\n1275" ans))
            (is (true? (.isClosed socket)))
            (reset! running nil)
            (Thread/sleep 500)))))))
