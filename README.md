<img src="assets/logo/logo-alt.svg" width="200px">

![master](https://github.com/alekcz/pcp/workflows/master/badge.svg) ![codecov](https://codecov.io/gh/alekcz/pcp/branch/master/graph/badge.svg) ![downloads](https://img.shields.io/github/downloads/alekcz/pcp/total)  

**Welcome to PCP**

> PCP: Clojure Processor -- _Like drugs but better_  

## Introduction

Too long have we hustled to deploy Clojure website. Too long have we spun up one instance per site. Too long have reminisced about PHP. Today we enjoy the benefits of both. Welcome to PCP.

### Status
Active development. Stabilizing.    

### Goals

* Any easy to use, drop-in Clojure replacement for php scripts
* Allow multiple website to be hosted on single $5 VPS

### Non-goals

* Performance.  _PCP should be sufficient for prototypes and small websites  (<= 400 req/s)_

### How PCP works
PCP has two parts the utility is simple binary, built with GraalVM, that allows you work effectively with pcp. 
```
PCP: Clojure Processor -- Like drugs but better

Usage: pcp [option] [value]

Options:
  new [project]           Create a new pcp project in the [project] directory
  service [stop/start]    Stop/start the PCP SCGI server daemon
  passphrase [project]    Set passphrase for [project]
  secret [path]           Add and encrypt secrets at . or [path]
  secret [path]           Add and encrypt secrets at . or [path]
  -e, --evaluate [path]   Evaluate a clojure file using PCP
  -s, --serve [root]      Start a local server at . or [root]
  -v, --version           Print the version string and exit
  -h, --help              Print the command line help
```      
The heavy lifting is done by an [SCGI](https://en.wikipedia.org/wiki/Simple_Common_Gateway_Interface) server. This server runs on port 9000 and receives requests from the local pcp server or from nginx. The SCGI server is an uber jar that runs as a daemon.

### Demo site

You can view a demo site built in PCP here: [https://pcp-demo.musketeers.io/](https://pcp-demo.musketeers.io/)

## Quick start
Install pcp via the installer script:
``` shellsession
$ bash -c "$(curl -sSL https://raw.githubusercontent.com/alekcz/pcp/master/install.sh)"
```

Query the SCGI server status:
``` shellsession
$ pcp service status
```

Create a new project:
``` shellsession
$ pcp new project-name
```

For convenience the pcp utility comes with a local server. This local sever behaves as your pcp site would when deployed with nginx. 

``` shellsession
$ cd project-name
$ pcp -s public/
```

You can find instructions on [replacing php here](./docs/replacing-php.md)

## Guides

1. [Installation](./docs/installation.md)
2. [Environment variables and secrets](#pcpsecret)
3. [Replacing php](./docs/replacing-php.md)
4. [Libraries included in PCP](#additional-namespaces)
5. [PCP demo site: Clojure Pulse](https://clojure-pulse.musketeers.io/)

## Project structure and requiring files

Requiring files in PCP mostly works as it does in Clojure, the primary exception being that a project cannot require a file outside of it's root. 
Given a project like this:
.
├── .pcp      
│   └── a24d...cfdcb8.npy  ; encrypted secret; touch this and bad things will happen
├── public                 ; project root
│   ├── index.clj          
│   └── login.clj
│   └── dashboard.clj
│   └── logic
│       └── orders.clj
│       └── payments.clj
│       └── stock.clj
│       └── users.clj
├── pcp.edn                ; pcp configuration file
└── README.md              ; surely you don't need a comment to tell you what this is.
    
   
Your dashboard could require business logic as follows
```clojure
(ns dashboard
  (:require '[logic.users :as lu]
            '[logic.orders :as lo]
            '[logic.stock :as ls]
            '[logic.payments :as lp]))
```


## Core PCP functions

The following functions are part of the core PCP namespace and are made available for convenience. 

#### pcp/persist
`(pcp/persist :cache-key  f-on-miss)`    
This macro allows expensive operations to only be recomputed if they are not in the cache.   
On a cache miss the `f-on-miss` is computed, its value stored in the cache and returned. Caches are isolated
across projects. Pages in the same project share a cache keys. The cache uses TTL (30 min) as its invalidation strategy. 

#### pcp/clear
`(pcp/clear :cache-key)`    
Removes key from the cache

#### pcp/request
`(pcp/request)`    
Returns the request map. The request map conforms to the [ring spec](https://github.com/ring-clojure/ring/blob/master/SPEC). 

#### pcp/response
`(pcp/response [status body mime-type])`        
A convenience function for generating a response map. Responses are simply Clojure maps that confirm to the [ring spec](https://github.com/ring-clojure/ring/blob/master/SPEC) and can be written by hand too. 

#### pcp/render-html
`(pcp/render-html options & content)`    
Renders html from Clojure data strucutures using [hiccup](https://github.com/weavejester/hiccup)

#### pcp/render-html-unescaped
`(pcp/render-html & args)`    
Renders html from Clojure data strucutures using [hiccup](https://github.com/weavejester/hiccup). Does not escape html tags strings. Use with care.  

#### pcp/secret
`(pcp/secret "SECRET_NAME")`    
Retrieves secret from project. The secret is read from disk. It may be worthwhile using `pcp/persist` to improve performance. 

To secure allow API keys and the like to be stored and retrieve securely. Secrets are created using the PCP CLI and store in the project.
They are encrypted using the passphrase selected.  

```shellsession
$ pcp secret
--------------------------------------------------
Set an encrypted secret variable for project: pcp-demo
Please ensure you use the same passphrase for all your secrets in this project
and that you add your passphrase to your production server using:
  pcp passphrase pcp-demo
--------------------------------------------------
Secret name: GITHUB_PERSONAL_ACCESS_TOKEN
Secret value: ghp_eR17e5vHq0Sdmj22oracJd0Y7je1IM3g7oPV7yT7sq31
Passphrase: my-super-secure-passphrase-that-i-will-also-store-on-the-server    
encrypting...
done.
```

#### pcp/now
`(pcp/now)`    
Returns the current time in milliseconds (according to your server).

## Additional Namespaces

In addition to the core clojure namespaces available in [sci](https://github.com/borkdude/sci), the following namespaces are also available.

  - `clojure.string`
  - `clojure.core.async` 
  - `clojure.edn`
  - `cheshire.core`
  - `selmer.parser`
  - `selmer.filters`
  - `org.httpkit.client`
  - `org.httpkit.sni-client`
  - `clj-http.client`
  - `next.jdbc`
  - `honeysql.core`
  - `honeysql.helpers`
  - `postal.core`
  - `tick.alpha.api`
  - `buddy.sign.jwt` 
  - `buddy.sign.jwe`
  - `buddy.core.hash`
  - `buddy.core.codecs`
  - `buddy.core.keys` 
  - `buddy.auth.backends`
  - `buddy.auth.middleware`
  - `buddy.hashers` 
  - `garden.core` 
  - `garden.stylesheet`
  - `garden.units` 
  - `konserve.core`
  - `konserve.filestore`
  - `konserve-jdbc.core` 
    



## Special thanks
For the guidance and examples, special thanks to

- [@BrunoBonacci](https://github.com/BrunoBonacci) 
- [@borkdude](https://github.com/borkdude) 

## License

Copyright © 2020 Alexander Oloo

PCP is licensed under the MIT License.