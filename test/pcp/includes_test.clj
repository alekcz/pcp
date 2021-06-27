(ns pcp.includes-test
  (:require [clojure.test :refer [deftest is testing]]
            [pcp.includes :as includes]))

(deftest extract-namespace-test
  (testing "Test extracting namespaces"
    (let [result (includes/extract-namespace 'pcp.includes)
          ans {'includes #'pcp.includes/includes, 'extract-namespace #'pcp.includes/extract-namespace}]
      (is (= ans result)))))