(ns pcp.includes
  (:require
    [clojure.string :as str]
    ;included in environment
    [cheshire.core]
    [selmer.parser]
    [selmer.filters]
    [clj-http.lite.client]
    [next.jdbc]
    [honeysql.core]
    [honeysql.helpers]
    [postal.core]
    [clojurewerkz.scrypt.core])
  (:gen-class))

(set! *warn-on-reflection* 1)


(defn extract-namespace [namespace]
  (into {} (ns-publics namespace)))

 (defn html [v]
  (cond (vector? v)
        (let [tag (first v)
              attrs (second v)
              attrs (when (map? attrs) attrs)
              elts (if attrs (nnext v) (next v))
              tag-name (name tag)]
          (format "<%s%s>%s</%s>\n" tag-name (html attrs) (html elts) tag-name))
        (map? v)
        (str/join ""
                  (map (fn [[k v]]
                        (if (nil? v)
                          (format " %s" (name k))
                          (format " %s=\"%s\"" (name k) v))) v))
        (seq? v)
        (str/join " " (map html v))
        :else (str v))) 

(def includes
  { 
    'clojurewerkz.scrypt.core (extract-namespace 'clojurewerkz.scrypt.core)
    'postal.core (extract-namespace 'postal.core)
    'selmer.parser (extract-namespace 'selmer.parser)
    'selmer.filters (extract-namespace 'selmer.filters)
    'clj-http.lite.client (extract-namespace 'clj-http.lite.client)
    'next.jdbc (extract-namespace 'next.jdbc)
    'honeysql.core (extract-namespace 'honeysql.core)
    'honeysql.helpers (extract-namespace 'honeysql.helpers)                 
    'cheshire.core (extract-namespace 'cheshire.core)})
