ALTER TABLE playlist_streams
  DROP CONSTRAINT pk_playlist_streams;

--;;
ALTER TABLE playlist_streams
  ADD CONSTRAINT pk_playlist_streams PRIMARY KEY (playlist_id, playlist_stream_order);
