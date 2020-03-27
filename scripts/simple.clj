(require '[clj-http.lite.client :as client]
         '[cheshire.core :as json]
         '[clojure.pprint :refer [pprint]])
         
(let [resp (client/get "https://jsonplaceholder.typicode.com/users/")
      todo (json/decode  (:body resp) true)]
    (println "Today's itinerary:")
    (pprint todo)
    (println "Today's itinerary:")
    (println (slurp "./test.txt")))