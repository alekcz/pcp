(require  '[cheshire.core :as json]
          '[pcp :as pcp])

(def names ["pcp" "sci" "clojure"])

(pcp/response 200  
  (json/encode {:name "Test" 
                :num (apply + (range 51))
                :end nil}) "application/json")