# Welcome to PCP

> PCP: Clojure Processor -- _Like drugs but better_  
  
![master](https://github.com/alekcz/pcp/workflows/master/badge.svg) [![codecov](https://codecov.io/gh/alekcz/pcp/branch/master/graph/badge.svg)](https://codecov.io/gh/alekcz/pcp)

## Introduction

Too long have we hustled to deploy clojure website. Too long have we spun up one instance per site. Too long have reminisced about PHP. Today we enjoy the benefits of both. Welcome to PCP.

### Status
Experimental. Flux. Active development. Chaos.    

### Goals

* Any easy to use, drop-in replacement for php scripts
* Allow multiple website to be hosted on single $5 VPS

### Non-goals

* Performance.  __PCP should be sufficient for prototypes and small websites  (<= 40 req/s)__

### How PCP works
PCP has two parts
- The utility
- The SCGI server

#### The utility

```
PCP: Clojure Processor -- Like drugs but better

Usage: pcp [option] [value]

Options:
  service [stop/start]    Stop/start the PCP SCGI server daemon
  -e, --evaluate [path]   Evaluate a clojure file using PCP
  -s, --serve [root]      Start a local server at . or [root]
  -v, --version           Print the version string and exit
  -h, --help              Print the command line help
```      

#### The SCGI Server

### Available libraries
The following libraries are available in the pcp environment. 

```clojure
(require  '[cheshire.core :as json]
          '[selmer.parser :as parser]
          '[selmer.filters :as filters]
          '[clj-http.lite.client :as client]
          '[next.jdbc :as jdbc]
          '[honeysql.core :as sql]
          '[honeysql.helpers :as helpers]
          '[postal.core :as email]
          '[clojurewerkz.scrypt.core :as sc])
```

### Talk

None yet.

## Getting Started

### Installer script

Install via the installer script:

``` shellsession
$ bash -c "$(curl -sSL https://raw.githubusercontent.com/alekcz/pcp/master/install.sh)"
```

### Running scripts

Read the output from a shell command as a lazy seq of strings:

``` shell
$ pcp /path/to/file.clj
```

### Local Server


### Replacing php-fpm
We need to modify the server block in `/etc/nginx/sites-enabled/digitalocean` to switch from `php` to `pcp`.   
Note: `pcp` uses SimpleCGI instead of FastCGI.

Change the index to be `index.clj` instead of `index.php`
```conf
...
    #default nginx config on digital ocean lemp image
    #index index.php index.html index.htm;

    #new pcp config => change .php to .clj
    index index.clj index.html index.htm;
...
```

Send our scripts to our SimpleFGI Server at port 9000 and change the filter to be `~ \.clj$` instead of `~ \.php$`.  
```conf
...
    #default nginx config on digital ocean lemp image
    #location ~ \.php$ {

    #new pcp config => change .php to .clj
    location ~ \.clj$ {
        #default nginx config on digital ocean lemp image
        #include snippets/fastcgi-php.conf;
        #fastcgi_pass unix:/run/php/php7.0-fpm.sock;

        include scgi_params;
        scgi_intercept_errors on;
        scgi_pass   127.0.0.1:9000;
    }
...
```

Let's restart nginx with our new configuration. 
```bash
$ service nginx restart
```

Now that we've switched to `pcp` let generate a ton of requests using [artillery](https://artillery.io/).

```bash
$  artillery quick -d 30 -r 400 http://pcp.musketeers.io/

...some output ommitted...

All virtual users finished
Summary report @ 08:05:48(+0200) 2020-03-31
  Scenarios launched:  12000
  Scenarios completed: 12000
  Requests completed:  12000
  Mean response/sec: 136.85
  Response time (msec):
    min: 551.4
    max: 4758.2
    median: 674.1
    p95: 1265.2
    p99: 2468.1
  Scenario counts:
    0: 12000 (100%)
  Codes:
    200: 12000
```

And would you look at that. You're running clojure and your server didn't even flinch. 


## Special thanks
For the guidance and examples, special thanks to

- [Bruno Bonacci](https://github.com/BrunoBonacci) 
- [Michiel Borkent](https://github.com/borkdude) 

## License

Copyright © 2020 Alexander Oloo

This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary Licenses when the conditions for such availability set forth in the Eclipse Public License, v. 2.0 are satisfied: GNU General Public License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any later version, with the GNU Classpath Exception which is available at https://www.gnu.org/software/classpath/license.html.