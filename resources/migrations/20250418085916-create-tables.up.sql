CREATE TABLE IF NOT EXISTS users (
  id bigserial,
  username varchar(50) UNIQUE NOT NULL,
  session_id varchar(36) NULL,
  password TEXT NOT NULL,
  created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_users PRIMARY KEY (id)
);

--;;
CREATE TABLE IF NOT EXISTS channels (
  id bigserial,
  url text UNIQUE NULL,
  name varchar(255) NULL,
  avatar varchar(255) NULL,
  verified bool NULL,
  CONSTRAINT pk_channels PRIMARY KEY (id)
);

--;;
CREATE TABLE IF NOT EXISTS playlists (
  id bigserial,
  playlist_id uuid NOT NULL UNIQUE DEFAULT gen_random_uuid (),
  name varchar(255) NOT NULL,
  thumbnail varchar(255) NULL,
  owner INT8 NOT NULL,
  CONSTRAINT pk_playlists PRIMARY KEY (id),
  CONSTRAINT fk_playlists_owner FOREIGN KEY (OWNER) REFERENCES users (id)
);

--;;
CREATE TABLE IF NOT EXISTS streams (
  id bigserial,
  url text UNIQUE NULL,
  duration bigint NULL,
  thumbnail varchar(255) NULL,
  name varchar(255) NULL,
  channel_id bigserial NOT NULL,
  CONSTRAINT pk_streams PRIMARY KEY (id),
  CONSTRAINT fk_streams_channel_id FOREIGN KEY (channel_id) REFERENCES channels (id)
);

--;;
CREATE TABLE IF NOT EXISTS playlist_streams (
  stream_id bigserial NOT NULL,
  playlist_id bigserial NOT NULL,
  playlist_stream_order bigint NOT NULL,
  CONSTRAINT pk_playlist_streams PRIMARY KEY (playlist_id, playlist_stream_order),
  CONSTRAINT fk_playlists FOREIGN KEY (playlist_id) REFERENCES playlists (id),
  CONSTRAINT fk_streams FOREIGN KEY (stream_id) REFERENCES streams (id)
);
