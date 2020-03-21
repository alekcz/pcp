(ns pcp-engine.cli
  (:gen-class))


(defn list-sites []
    (println "Sites: "))

(defn login [server]
    (println (str "Loggin into " server)))

(defn logout []
    (println "Logging out..."))   

(defn deploy-site [site]
    (println (str "Deploying " site)))    