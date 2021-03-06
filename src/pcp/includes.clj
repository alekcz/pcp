(ns pcp.includes
  (:require
    ;included in environment
    [clojure.string]
    [clojure.edn]
    [cheshire.core]
    [selmer.parser]
    [selmer.filters]
    [org.httpkit.client]
    [org.httpkit.sni-client]
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
    [buddy.hashers]
    [storyblok-clj.core]
    [garden.core]
    [garden.stylesheet]
    [garden.units]
    [konserve.filestore]
    [konserve.core]
    [clojure.core.async]
    [konserve-jdbc.core]
    [clj-uuid])
  (:gen-class))

(set! *warn-on-reflection* 1)


(defn extract-namespace [namespace]
  (into {} (ns-publics namespace)))

(def includes
  { 
    'clojure.string (extract-namespace 'clojure.string)
    'clojure.edn (extract-namespace 'clojure.edn)
    'cheshire.core (extract-namespace 'cheshire.core)
    'selmer.parser (extract-namespace 'selmer.parser)
    'selmer.filters (extract-namespace 'selmer.filters)
    'org.httpkit.client (extract-namespace 'org.httpkit.client)
    'org.httpkit.sni-client (extract-namespace 'org.httpkit.sni-client)
    'storyblok-clj.core (extract-namespace 'storyblok-clj.core)
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
    'buddy.hashers (extract-namespace 'buddy.hashers)
    'garden.core (extract-namespace 'garden.core)
    'garden.stylesheet (extract-namespace 'garden.stylesheet)
    'garden.units (extract-namespace 'garden.units)
    'konserve.core (extract-namespace 'konserve.core)
    'konserve.filestore (extract-namespace 'konserve.filestore)
    'konserve-jdbc.core (extract-namespace 'konserve-jdbc.core)
    'clojure.core.async (extract-namespace 'clojure.core.async)
    'clj-uuid (extract-namespace 'clj-uuid)})
