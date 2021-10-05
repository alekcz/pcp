# Running PCP using containers

You can run `pcp` using container orchestrators

* Docker
* podman
* [TODO] Kubernetes

## Building container image using docker/podman


```sh
    # Build the pcp container
    docker build -t pcp .
```

## Using pcp container

You can run the container as is:

```sh
    # Use the container using the default site
    docker run --rm -ti -p 3000:3000 pcp

    # Mount your site in the container. Once it starts, you can edit files and refresh.
    docker run --rm -ti -p 3000:3000 -v examples/netdava-test/public:/usr/share/pcp-site pcp
```

You can build a custom container from pcp with your site.

```sh

cat > mysite.dockerfile << EOF
FROM pcp:latest
COPY my-webiste /usr/share/pcp-site
EOF

# Build the custom image
docker build -t my-pcp-site:latest -f mysite.dockerfile .
# Run your website

docker run --name my-pcp-site -p 3000:3000 my-pcp-site:latest
```

