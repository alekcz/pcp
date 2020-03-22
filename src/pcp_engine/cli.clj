(ns pcp-engine.cli
   (:require
     [clj-http.lite.client :as client])
  (:gen-class))


(defn list-sites []
    (println "Sites: "))

(defn login [server]
  (println (str "Connecting to " server)))

(defn logout []
    (println "Logging out..."))   

(defn deploy-site [site]
    (println (str "Deploying " site)))    

(defn start-server [root]
    (println (str "starting server " root)))    