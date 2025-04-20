CREATE TABLE IF NOT EXISTS users (
  id bigserial,
  username varchar(50) UNIQUE NOT NULL,
  password TEXT NOT NULL,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_users PRIMARY KEY (id)
);

--;;
CREATE TABLE IF NOT EXISTS channels (
  url text,
  name varchar(255) NULL,
  avatar varchar(255) NULL,
  verified bool NULL,
  CONSTRAINT pk_channels PRIMARY KEY (url)
);

--;;
CREATE TABLE IF NOT EXISTS playlists (
  id bigserial,
  name varchar(255) NOT NULL,
  thumbnail varchar(255) NULL,
  owner INT NOT NULL,
  CONSTRAINT pk_playlists PRIMARY KEY (id),
  CONSTRAINT fk_playlists_owner FOREIGN KEY (OWNER) REFERENCES users (id)
);

--;;
CREATE TABLE IF NOT EXISTS streams (
  url text,
  duration bigint NULL,
  thumbnail varchar(255) NULL,
  name varchar(255) NULL,
  uploader_url text NULL,
  CONSTRAINT pk_streams PRIMARY KEY (url),
  CONSTRAINT fk_streams_uploader_url FOREIGN KEY (uploader_url) REFERENCES channels (url)
);

--;;
CREATE TABLE IF NOT EXISTS playlist_streams (
  stream_url text NOT NULL,
  playlist_id bigserial NOT NULL,
  CONSTRAINT fk_playlists FOREIGN KEY (playlist_id) REFERENCES playlists (id),
  CONSTRAINT fk_streams FOREIGN KEY (stream_url) REFERENCES streams (url)
);

--;;
CREATE TABLE IF NOT EXISTS session_store (
  session_id varchar(36) NOT NULL PRIMARY KEY,
  idle_timeout bigint,
  absolute_timeout bigint,
  value bytea
);
