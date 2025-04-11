FROM clojure:tools-deps AS build

RUN mkdir /app
WORKDIR /app

RUN apt-get update && apt-get install -y npm

COPY package* /app/
RUN npm install

COPY . /app
RUN clojure -M:cljs release frontend

FROM nginx:stable-alpine

COPY --from=build /app/resources/public /usr/share/nginx/html/

COPY docker/frontend/nginx.conf /etc/nginx/conf.d/default.conf

COPY docker/frontend/entrypoint.sh /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
