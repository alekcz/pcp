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
