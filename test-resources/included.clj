(ns included)

(def test 100)

(defn working? []
  (every? even? '(2 4 6)))