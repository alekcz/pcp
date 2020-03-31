# pcp
PCP: Clojure Processor
__Like drugs but better__


## Installation

Download from http://example.com/FIXME.

## Usage

### 1. Run a local script
```
$ pcp path/to/script.clj
```

### 2. Run a local server
```
pcp serve [path-to-server-root]
```

### 3. Replacement for php



## Options

FIXME: listing of options this app accepts.

## Examples

```bash
$ nano script.clj
```
Paste this conten
```clojure
; 
(require '[clj-http.lite.client :as client]
         '[cheshire.core :as json]
         '[clojure.pprint :refer [pprint]])
         
(let [resp (client/get "https://jsonplaceholder.typicode.com/users/")
      users (json/decode  (:body resp) true)]
    (println "User info:")
    (pprint users))
```    

```bash
$ pcp script.clj
User info:
({:id 1,
  :name "Leanne Graham",
  :username "Bret",
  :email "Sincere@april.biz",
  :address
  {:street "Kulas Light",
   :suite "Apt. 556",
   :city "Gwenborough",
   :zipcode "92998-3874",
   :geo {:lat "-37.3159", :lng "81.1496"}},
  :phone "1-770-736-8031 x56442",
  :website "hildegard.org",
  :company
  {:name "Romaguera-Crona",
   :catchPhrase "Multi-layered client-server neural-net",
   :bs "harness real-time e-markets"}}
 {:id 2,
  :name "Ervin Howell",
  :username "Antonette",
  :email "Shanna@melissa.tv",
  :address
...
```


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
$  artillery quick -d 30 -r 400 http://pcp.org/

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

### Bugs
- At the moment postgres connection can't use SSL. Life isn't perfect. 
...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2020 Alexander Oloo

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
