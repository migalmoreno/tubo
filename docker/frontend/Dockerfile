FROM clojure:tools-deps AS build

RUN mkdir /app
WORKDIR /app

RUN apt-get update && apt-get install -y npm

COPY package* /app/
RUN npm install

COPY . /app
RUN clojure -M:cljs release frontend

FROM nginxinc/nginx-unprivileged:alpine

COPY --from=build /app/resources/public /usr/share/nginx/html/

EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]
