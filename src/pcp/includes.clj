(ns pcp.includes
  (:require
    ;included in environment
    [clojure.string :as str]
    [cheshire.core]
    [selmer.parser]
    [selmer.filters]
    [clj-http.lite.client]
    [next.jdbc]
    [honeysql.core]
    [honeysql.helpers]
    [postal.core]
    [tick.alpha.api]
    [buddy.sign.jwt]
    [buddy.sign.jwe]
    [buddy.core.hash]
    [buddy.core.codecs]
    [buddy.core.keys]
    [buddy.auth.backends]
    [buddy.auth.middleware]
    [buddy.hashers])
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
    'clojure.string (extract-namespace 'clojure.string)
    'cheshire.core (extract-namespace 'cheshire.core)
    'selmer.parser (extract-namespace 'selmer.parser)
    'selmer.filters (extract-namespace 'selmer.filters)
    'clj-http.lite.client (extract-namespace 'clj-http.lite.client)
    'next.jdbc (extract-namespace 'next.jdbc)
    'honeysql.core (extract-namespace 'honeysql.core)
    'honeysql.helpers (extract-namespace 'honeysql.helpers)                 
    'postal.core (extract-namespace 'postal.core)
    'tick.alpha.api (extract-namespace 'tick.alpha.api)
    'buddy.sign.jwt (extract-namespace 'buddy.sign.jwt)
    'buddy.sign.jwe (extract-namespace 'buddy.sign.jwe)
    'buddy.core.hash (extract-namespace 'buddy.core.hash)
    'buddy.core.codecs (extract-namespace 'buddy.core.codecs)
    'buddy.core.keys (extract-namespace 'buddy.core.keys)
    'buddy.auth.backends (extract-namespace 'buddy.auth.backends)
    'buddy.auth.middleware (extract-namespace 'buddy.auth.middleware)
    'buddy.hashers (extract-namespace 'buddy.hashers)})
