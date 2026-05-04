FROM clojure:tools-deps AS build

RUN mkdir /app
WORKDIR /app

RUN apt-get update && apt-get install -y curl && \
    curl -fsSL https://deb.nodesource.com/setup_lts.x | bash - && \
    apt-get install -y nodejs

COPY package* /app/
RUN npm install

COPY . /app
RUN clojure -M:cljs release bg-helper

CMD ["node", "resources/public/js/bg-helper.js"]
