CREATE TABLE IF NOT EXISTS subscriptions (
  user_id bigserial NOT NULL,
  channel_id bigserial NOT NULL,
  subscribed_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT pk_user_subscriptions PRIMARY KEY (user_id, channel_id),
  CONSTRAINT fk_subscriptions_channel_id FOREIGN KEY (channel_id) REFERENCES channels (id)
);
