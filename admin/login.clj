(include "components.clj")

(def main 
    [:form {:class "login content is-large"}  
        [:h6 {:class "has-text-weight-normal"} 
            [:strong "Login"]]
        [:p {:class "is-size-5"} ]
        [:div {:class "field"}
            [:label {:class "is-size-6"} "Username"]
            [:div {:class "control"}
                [:input {:class "input" :type "text" :placeholder "admin" :name "username"}]]]
        [:div {:class "field"}
            [:label {:class "is-size-6"} "Password"]
            [:div {:class "control"}
                [:input {:class "input" :type "password" :placeholder "l33t-h4xx0r" :name "password"}]]]  
        [:div {:class "field"}      
            [:label {:class "is-size-6"} "&nbsp;"]                        
            [:div {:class "control"}                 
                (primary-button "Learn more" "https://github.com/alekcz/pcp")]]
     ])

(response 200  
    (page main {:auth false :show-login true}) "text/html")