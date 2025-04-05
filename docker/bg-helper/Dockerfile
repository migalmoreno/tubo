FROM clojure:tools-deps as build

RUN mkdir /app
WORKDIR /app

RUN apt-get update && apt-get install -y npm

COPY package* /app/
RUN npm install

COPY . /app
RUN clojure -M:cljs release bg-helper

CMD node resources/public/js/bg-helper.js
