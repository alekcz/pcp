# Welcome to PCP

## PCP: Clojure Processor 
__Like drugs but better__

## Introduction

Too long have we hustled to deploy clojure website. Too long have we spun up one instance per site. Too long have reminisced about PHP. Today we enjoy the benefits of both. Welcome to PCP.

### Goals

* Low latency Clojure scripting alternative to JVM Clojure.

### Non-goals

* Performance<sup>1<sup>
* Provide a mixed Clojure/Bash DSL (see portability).
* Replace existing shells. Babashka is a tool you can use inside existing shells like bash and it is designed to play well with them. It does not aim to replace them.

<sup>1<sup> Babashka uses [sci](https://github.com/borkdude/sci) for
interpreting Clojure. Sci implements a suffiently large subset of
Clojure. Interpreting code is in general not as performant as executing compiled
code. If your script takes more than a few seconds to run, Clojure on the JVM
may be a better fit, since the performance of Clojure on the JVM outweighs its
startup time penalty. Read more about the differences with Clojure
[here](#differences-with-clojure).


### How PCP works
There 



### Available libraries
The following libraries are available in the pcp environment. 

`[cheshire.core :as json]`
`[clostache.parser :as parser]`
`[clj-http.lite.client :as client]`
`[next.jdbc :as jdbc]`
`[honeysql.core :as sql]`
`[honeysql.helpers :as helpers]`
`[postal.core :as email]`
`[clojurewerkz.scrypt.core :as sc]`

### Talk

None yet.

## Getting Started

On Windows you can install using [scoop](https://scoop.sh/) and the
[scoop-clojure](https://github.com/littleli/scoop-clojure) bucket.

### Installer script

Install via the installer script:

``` shellsession
$ bash <(curl -s https://raw.githubusercontent.com/alekcz/pcp/master/install.sh)
```

### Running scripts

Read the output from a shell command as a lazy seq of strings:

``` shell
$ pcp /path/to/file.clj
```

### Local Server


### Setting up a server

Functionality regarding `clojure.core` and `java.lang` can be considered stable
and is unlikely to change. Changes may happen in other parts of babashka,
although we will try our best to prevent them. Always check the release notes or
[CHANGES.md](CHANGES.md) before upgrading.


## Thanks
For all the 

- [Bruno Bonacci](https://github.com/BrunoBonacci) 
- [Michiel Borkent](https://github.com/borkdude) 

## License

Copyright © 2020 Alexander Oloo

This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary Licenses when the conditions for such availability set forth in the Eclipse Public License, v. 2.0 are satisfied: GNU General Public License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any later version, with the GNU Classpath Exception which is available at https://www.gnu.org/software/classpath/license.html.