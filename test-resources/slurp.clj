(require '[pcp :as pcp])

(def r (rand))
(pcp/spit "../tmp/random.txt" r)
(if (= (str r) (pcp/slurp "../tmp/random.txt"))
  (pcp/slurp "slurp.txt")
  nil)