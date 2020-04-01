(defn primary-button [text target]
    [:a {:class "button is-primary" :href target} text])

(defn secondary-button [text target]
    [:a {:class "button is-light" :href target} text])


(defn header [state]
    [:nav {:class "navbar"} 
      [:div {:class "navbar-brand"} 
        [:a {:class "navbar-item" :href "/"} "PCP"]]
      [:a {:class "navbar-burger burger" :data-target "main-menu"}
        [:span {:aria-hidden true} ""]
        [:span {:aria-hidden true} ""]
        [:span {:aria-hidden true} ""]]
      [:div {:class "navbar-menu" :id "main-menu"} 
        (if (:auth state)
          [:div {:class "navbar-start"}
            [:a {:class "navbar-item" :href "/sites.clj"} "Sites"]
            [:a {:class "navbar-item" :href "/users.clj"} "Users"]]
          [:div {:class "navbar-start"}
            [:a {:class "navbar-item" :href "/"} "Home"]
            [:a {:class "navbar-item" :href "https://github.com/alekcz/pcp"} "Documentation"]])
        [:div {:class "navbar-end"}
          [:div {:class "navbar-item"}
            [:div {:class "navbar-item"}
              (if (:show-login state)
                [:div {:class "buttons"}
                  (if (:auth state)
                    (secondary-button "Logout" "/login.clj")
                    (primary-button "Login" "/login.clj"))])]]]]])

(def footer 
    [:div {:class "footer"} ""])

(defn page [main auth]
  (str
    "<!DOCTYPE html>"
    (html
      [:html {:lang "en"}
        [:head
            "<meta charset='utf-8'>"
            "<meta name='viewport' content='width=device-width, initial-scale=1'>"
            [:link {:rel "stylesheet" :type "text/css" :href "https://fonts.googleapis.com/css2?family=Ubuntu:wght@400&display=swap"}]
            [:link {:rel "stylesheet" :type "text/css" :href "bulma.css"}]
            [:link {:rel "stylesheet" :type "text/css" :href "core.css"}]
            [:script {:src "https://use.fontawesome.com/releases/v5.3.1/js/all.js" :defer nil}]]
        [:body 
            (header auth)
            [:div {:class "main section"} main]
            footer]])))

