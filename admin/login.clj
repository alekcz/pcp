(include "components.clj")

(def main 
    [:div {:class "container"}  
        [:h1 {:class "title"} "Welcome to the PCP Admin Panel"]
        [:form {:class "form"}
            (primary-button "Login" "/admin.clj")]
     ])

(response 200  
    (page main {:auth false :show-login false}) "text/html")