FROM clojure:tools-deps

RUN mkdir /app
WORKDIR /app
COPY deps.edn /app

RUN clojure -P
COPY . /app

ENV TUBO_ENV=prod

CMD ["clojure", "-M:run"]
