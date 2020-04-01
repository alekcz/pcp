(require '[cheshire.core :as json])


(def names ["Alexander" "Alex" "Al"])

(def header 
    [:div {:class "header"} ""])
(def main 
    [:div {:class "main"}  
        [:h1 {:class "title"} "Welcome to the PCP Admin Panel"]
        [:a {:class "button is-primary"} "Login"]
       ; [:div 
            ; (str
            ; (email/send-message 
            ;     {   :host "mail.musketeers.io"
            ;         :user "pcp@musketeers.io"
            ;         :pass "@drug4rebad"}
            ;     {   :from "alex@pcp-clj.com"
            ;         :to ["alekcz@gmail.com"]
            ;         :subject "Welcome to PCP!"
            ;         :body "Testing. Testing 1 2."}) )
                   ; ]
                    ])
(def footer 
    [:div {:class "footer"} ""])

(defn page [header main footer]
    [:html
      [:head
        "<meta charset='utf-8'>"
        "<meta name='viewport' content='width=device-width, initial-scale=1'>"
        [:link {:rel "stylesheet" :type "text/css" :href "https://fonts.googleapis.com/css2?family=Ubuntu:wght@300&display=swap"}]
        [:link {:rel "stylesheet" :type "text/css" :href "bulma.css"}]
        [:link {:rel "stylesheet" :type "text/css" :href "core.css"}]

        [:script {:src "https://use.fontawesome.com/releases/v5.3.1/js/all.js" :defer nil}]
        ]
      [:body 
        header 
        main 
        footer]])


(response 200  
    (str "<!DOCTYPE html>" (html (page header main footer))) "text/html")