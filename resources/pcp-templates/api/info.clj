(ns api.info
  (:require [cheshire.core :as json]
            [pcp :as pcp]))

(def names ["pcp" "sci" "clojure"])
(def repo "https://github.com/alekcz/pcp")

(pcp/response 
  200  
  (json/encode {:engine (first names)
                :interpreter (second names)
                :core (last names)
                :repo repo}) 
  "application/json")