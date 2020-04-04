(require '[cheshire.core :as json]
         '[clostache.parser :as parser])    

(def names ["pcp" "sci" "clojure"])

(response 200  
    (json/encode {:name "Alex Oloo" 
                  :clostache (parser/render "Hello {{#names}}{{.}} {{/names}}" {:names names})
                   :end nil}) "text/plain")