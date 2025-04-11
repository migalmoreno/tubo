#!/bin/sh

if [ -z "${BACKEND_URL}" ]; then
    echo "BACKEND_URL not set"
    exit 1
fi

find /usr/share/nginx/html \
     -type f \( -name '*.js' \) \
     -exec sed -i "s@http://localhost:3000@${BACKEND_URL}@g" '{}' +

nginx -g "daemon off;"
