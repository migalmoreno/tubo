FROM clojure:tools-deps AS build

RUN mkdir /app
WORKDIR /app

RUN apt-get update && apt-get install -y curl && \
    curl -fsSL https://deb.nodesource.com/setup_lts.x | bash - && \
    apt-get install -y nodejs

COPY package*.json /app/
RUN npm install

COPY . /app
RUN --mount=type=cache,id=maven,target=/root/.m2 \
    --mount=type=cache,id=gitlibs,target=/root/.gitlibs \
    clojure -M:cljs release frontend

FROM nginx:stable-alpine

COPY --from=build /app/resources/public /usr/share/nginx/html/

COPY docker/frontend/nginx.conf /etc/nginx/conf.d/default.conf

COPY docker/frontend/entrypoint.sh /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
