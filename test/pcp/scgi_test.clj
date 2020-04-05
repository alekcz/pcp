(ns pcp.scgi-test
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [pcp.scgi :as scgi]
            [pcp.core :as core]
            [clojure.java.io :as io])
  (:import  [java.net Socket]
            [java.io Writer]))

(deftest serve-test
  (testing "FIXME, I fail."
    (let [running (atom true)
          handler #(core/scgi-handler %)
          _ (future (scgi/serve handler 55555 running))
          socket (Socket. "127.0.0.1",55555)
          message (slurp "test-resources/scgi-request.txt")
          len (count message)]
      (with-open [^Writer w (io/writer socket)
                    rdr (io/reader socket)]
        (.write w message 0 len)
        (.flush w)
        (let [ans (slurp rdr)]
          (is (= "200\r\nContent-Type: text/plain\r\n\r\n1275" ans))
          (is (true? (.isClosed socket)))
          (reset! running nil)
          (Thread/sleep 500))))))
