(require '[included :as i])
(require '[pcp :as pcp])

(pcp/response 
  200 
  {:ans (+ 5578 i/tester)
   :working (i/working?)}
  "text/plain")
