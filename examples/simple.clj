(require '[clj-http.lite.client :as client]
         '[cheshire.core :as json]
         '[clojure.pprint :refer [pprint]])
         
(let [resp (client/get "https://jsonplaceholder.typicode.com/users/")
      users (json/decode  (:body resp) true)]
    (println "User info:")
    (pprint users)
    (println "Today's itinerary:")
    (println (slurp "./test.txt")))