(require  '[cheshire.core :as json]
          '[pcp :as pcp])

(def names ["pcp" "sci" "clojure"])

(pcp/response 
  200  
  (json/encode {:engine (first names)
                :interpreter (second names)
                :core (last names)}) 
  "application/json")