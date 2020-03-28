(require '[cheshire.core :as json]
         '[selmer.parser :as selmer])

(include "/db/connect.clj")
(response 200  (selmer/render  "Hello {{name}} {{other}}!" {:name "Alex 2" :other other}) "text/plain")