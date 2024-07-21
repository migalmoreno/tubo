FROM clojure:tools-deps as build

RUN mkdir /app
WORKDIR /app

RUN apt-get update && apt-get install -y npm

COPY package* /app/
RUN npm install

COPY . /app
RUN npm run build
RUN clojure -M:frontend release tubo

FROM clojure:tools-deps
RUN mkdir /app
WORKDIR /app
COPY deps.edn /app

RUN clojure -P

COPY . /app
RUN clojure -M -e "(compile 'tubo.downloader-impl)"
COPY --from=build /app/resources /app/resources

EXPOSE 3000

CMD clojure -M:run
