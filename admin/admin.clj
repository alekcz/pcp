(include "components.clj")

(def main 
    [:div {:class "container"}  
        [:h1 {:class "title"} "Welcome to the PCP Admin Panel"]
        [:code (-> pcp :body)]
        [:br]
        (primary-button "Login" "/login.clj")
     ])

(response 200  
    (page main {:auth true :show-login true}) "text/html")