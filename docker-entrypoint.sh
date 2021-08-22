#!/bin/bash
#

# turn on bash's job control
set -m

# Start the primary process and put it in the background
java -jar /usr/local/bin/pcp-server.jar &

# Start the helper process
/usr/local/bin/pcp -s /usr/share/pcp-site

# now we bring the primary process back into the foreground
# and leave it there
fg %1