(require  '[pcp :as pcp])

(def resp 
  (pcp/html
    [:html {:style "font-family: 'Source Sans Pro', Arial, Helvetica, sans-serif;text-align: center;"
            :lang "en"}
      [:head 
        [:title "PCP website"]]
      [:body 
        [:div 
              {:style "display: flex; flex-direction: column; justify-content: center; align-items: center; min-height: 90vh;"
              :ondblclick "alert('Now we're cooking with gas')"}
              [:h1 "Your PCP site is up and running."]
              [:p "Your json endpoint is here " [:a {:href "/api.clj"} "here"] 
                [:br]
                "Learn more about pcp at " [:a {:href "https://github.com/alekcz/pcp"} "here"]
                [:br]
                "Happy scripting!"]]]]))

(pcp/response 200 resp "text/html")            