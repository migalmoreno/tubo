FROM clojure:tools-deps AS build

RUN mkdir /app
WORKDIR /app

RUN apt-get update && apt-get install -y curl && \
    curl -fsSL https://deb.nodesource.com/setup_lts.x | bash - && \
    apt-get install -y nodejs

COPY package* /app/
RUN npm install

COPY . /app
RUN --mount=type=cache,id=maven,target=/root/.m2 \
    --mount=type=cache,id=gitlibs,target=/root/.gitlibs \
    clojure -M:cljs release bg-helper

CMD ["node", "resources/public/js/bg-helper.js"]
