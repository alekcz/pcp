(require '[pcp :as pcp])

(pcp/response 200 (pcp/secret "SUPER_SECRET_API") "text/plain")