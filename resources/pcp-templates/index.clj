(ns index
  (:require [pcp :as pcp]
            [api.info :as a]
            [garden.core :refer [css]]))

(def resp 
  (pcp/html
    [:html {:style "font-family: 'Source Sans Pro', Arial, Helvetica, sans-serif;text-align: center;"
            :lang "en"}
      [:head 
        [:title "PCP website"]
        [:style 
          (css [:p.info { :background-color "#EEE" 
                          :font-size "13px"
                          :margin-top "40px"
                          :padding "10px" 
                          :text-align "left" 
                          :width "300px"}])
          (css [:strong { :font-size "13px"}])
          (css [:code   { :font-size "12px" 
                          :font-weight "normal"}])]]
      [:body 
        [:div 
              {:style "display: flex; flex-direction: column; justify-content: center; align-items: center; min-height: 90vh;"
              :ondblclick "alert('Now we're cooking with gas')"}
              [:img {:src "https://raw.githubusercontent.com/alekcz/pcp/master/assets/logo/logo-alt.svg" :width "200px"}]
              [:h1 "Your PCP site is up and running."]
              [:p "Your json endpoint is here " [:a {:href "/api/info.clj"} "here"] 
                [:br]
                "Learn more about pcp at " [:a {:href a/repo :target "_blank"} "here"]
                [:br]
                "Happy scripting!"]
              [:p.info
                [:strong "Request uri "] [:span (str (-> pcp/request :request-uri))]
                [:br] 
                [:strong "Request method "] [:span (str (-> pcp/request :request-method))]
                [:br] 
                [:strong "Request scheme "] [:span (str (-> pcp/request :request-scheme))]
                [:br] 
                [:strong "User agent "]
                [:br] 
                [:code (str (-> pcp/request :http-user-agent))]
                [:br]]]]]))

(pcp/response 200 resp "text/html")            
