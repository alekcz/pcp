(ns pcp.scgi-test
  (:require [clojure.test :refer :all]
            [pcp.scgi :as scgi]
            [pcp.core :as core]
            [clojure.java.io :as io])
  (:import  [java.net Socket InetAddress]
            [java.io Writer]))

(deftest serve-test
  (testing "Test SCGI server"
    (let [running (atom true)
          handler #(core/scgi-handler %)
          scgi-port 55555
          _ (future (scgi/serve handler scgi-port running))
          message (slurp "test-resources/scgi-request.txt")
          len (count message)]
      (Thread/sleep 500)
      (let [socket (Socket. (InetAddress/getByName "127.0.0.1") scgi-port)]
        (with-open [^Writer w (io/writer socket)
                      rdr (io/reader socket)]
          (.write w message 0 len)
          (.flush w)
          (let [ans (slurp rdr)]
            (is (= "200\r\nContent-Type: text/plain\r\n\r\n1275" ans))
            (is (true? (.isClosed socket)))
            (reset! running nil)
            (Thread/sleep 500)))))))