(require '[cheshire.core :as json]
         '[selmer.parser :as parser])    

;(include "/db/connect.clj")
(def names ["Alexander" "Alex" "Al"])
  
; (with-open [connection (jdbc/get-connection datasource)]
;     (println "create table" table-name)
;     (jdbc/execute! connection sql-create-table)
;     (println "inserting fruit into" table-name)
;     (jdbc/execute! connection sql-insert-fruits)
;     (jdbc/execute! connection honey-sql-insert-fruits)
;     ;(jdbc/execute! connection sql-delete-table)
;     (response 200  (json/encode {:name "Alex" 
;                                 :db (jdbc/execute! connection honey-sql-read-all-2)
;                                 :clostache (parser/render "Hello {{#names}}{{.}} {{/names}}" {:names names})
;                                 :end nil}) "text/plain"))

(response 200  
    (json/encode {:name "Alex" 
                  :clostache (parser/render "Hello {% for name in names %} {{name}} {% endfor%}" {:names names})
                 :end nil}) "text/plain")