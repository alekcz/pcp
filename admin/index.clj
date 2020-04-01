(include "components.clj")

(def main 
    [:div {:class "landing content is-large"}  
        [:h1 {:class "is-size-1 title"} "Welcome to PCP"]
        [:h6 {:class "has-text-weight-normal"} 
            [:strong "PCP Clojure Processor"]
            [:i {:class "has-text-weight-light"} "Like drugs but better"]]
        [:p {:class "is-size-5"} "Too long have we hustled to deploy clojure website. 
            Too long have we spun up one instance per site. Too long have reminisced about PHP. 
            Today we enjoy the benefits of both. Welcome to PCP."]
        (primary-button "Learn more" "https://github.com/alekcz/pcp")
     ])

(response 200  
    (page main {:auth false :show-login true}) "text/html")