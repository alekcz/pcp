# Replacing php-fpm  
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

One last check, to make sure that the pcp service is running:
``` shell
$ pcp service status
```

And now we can restart nginx with our new configuration. 
```shell
$ service nginx restart
```

Now that we've switched to `pcp` lets generate a ton of requests using [artillery](https://artillery.io/).

```bash
$  artillery quick -d 60 -r 200 http://pcp.musketeers.io/

...some output ommitted...

All virtual users finished
Summary report @ 08:16:45(+0200) 2020-04-12
  Scenarios launched:  12000
  Scenarios completed: 12000
  Requests completed:  12000
  Mean response/sec: 198.22
  Response time (msec):
    min: 451.5
    max: 705.3
    median: 472.2
    p95: 561.3
    p99: 623
  Scenario counts:
    0: 12000 (100%)
  Codes:
    200: 12000

```

And would you look at that. You're running clojure and your server didn't even flinch. 
