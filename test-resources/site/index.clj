(require '[cheshire.core :as json])    

(def names ["pcp" "sci" "clojure"])

(response 200  
  (json/encode {:name "Test" 
                :num (apply + (range 51))
                :end nil}) "application/json")