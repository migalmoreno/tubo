FROM clojure:tools-deps

RUN mkdir /app
WORKDIR /app
COPY deps.edn /app

RUN clojure -P
COPY . /app

CMD ["clojure", "-M:run"]
