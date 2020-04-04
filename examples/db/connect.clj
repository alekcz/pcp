 (require   '[next.jdbc :as jdbc]
            '[clojure.string :as str]
            '[honeysql.core :as sql]
            '[honeysql.helpers :refer [select insert-into columns values from]])

(def sql-create-table 
  ["CREATE TABLE  IF NOT EXISTS pcp (id SERIAL PRIMARY KEY, name VARCHAR(10) NOT NULL, appearance VARCHAR(10) NOT NULL, cost INT NOT NULL)"])

(def sql-read-all 
  ["SELECT * FROM pcp"])

(def honey-sql-read-all 
  (sql/format {:select [:*] :from [:pcp]}))

(def honey-sql-read-all-2 
  (sql/format (-> (select :*) (from :pcp)))) 

(def sql-delete-table 
  ["DELETE FROM pcp"])

(def sql-insert-fruits 
  ["INSERT INTO pcp (name, appearance, cost) VALUES ('Apple', 'rosy', 509), ('Pear', 'pearish', 428), ('Orange', 'round', 724)"])

(def honey-sql-insert-fruits
  (-> (insert-into :graalvm_test)
      (columns :name :appearance :cost)
      (values [ ["Grape" "tiny" 1]
                ["Mango" "odd" 312]
                ["Pineapple" "spiky" 956]])
      sql/format))

(def datasource (jdbc/get-datasource {  :dbtype "postgresql"
                                        :dbname "pcp"
                                        :user "pcp"
                                        :password "p@55w0rd"
                                        :host "localhost"
                                        :port 5432
                                        :useSSL false}))
(def table-name (str "pcp"))