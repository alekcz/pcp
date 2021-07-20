# Replacing php-fpm  
We need to modify the server block in `/etc/nginx/sites-enabled/digitalocean` to switch from `php` to `pcp`.   
Note: `pcp` uses nginx as a reverse proxy instead of FastCGI.

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
        proxy_pass http://127.0.0.1:9000;
        proxy_set_header document-root $document_root;
    }
...
```
Here's an [complete example](../resources/pcp.nginx.conf)

One last check, to make sure that the pcp service is running:
``` shell
$ pcp service status
```

And now we can restart nginx with our new configuration. 
```shell
$ service nginx restart
```

Would you look at that. You're running clojure and your server didn't even flinch. 
