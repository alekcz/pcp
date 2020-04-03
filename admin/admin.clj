(require '[clojurewerkz.scrypt.core :as sc])

(include "components.clj")

(def main 
    [:div {:class "container"}  
        [:h1 {:class "title"} "Welcome to the PCP Admin Panel"]
        [:br]
        ;[:p (sc/encrypt "secret" 16384 8 1)]
        (primary-button "Login" "/login.clj")
     ])

(response 200  
    (page main {:auth true :show-login true}) "text/html")