FROM clojure:tools-deps-alpine as build

RUN mkdir /app
WORKDIR /app

RUN apk add npm

COPY package* /app/
RUN npm install

COPY . /app
RUN npm run build
RUN clojure -M:frontend release tubo

FROM clojure:tools-deps-alpine
RUN mkdir /app
WORKDIR /app
COPY deps.edn /app

RUN clojure -P

COPY . /app
COPY --from=build /app/resources /app/resources

EXPOSE 3000

CMD clojure -M:run
