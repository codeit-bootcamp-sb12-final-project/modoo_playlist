SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM user_similarities;
DELETE FROM user_preference_tags;
DELETE FROM watching_sessions;
DELETE FROM user_content_interactions;
DELETE FROM playlist_contents;
DELETE FROM playlist_subscriptions;
DELETE FROM playlists;
DELETE FROM reviews;
DELETE FROM content_review_summaries;
DELETE FROM content_embeddings;
DELETE FROM content_tags;
DELETE FROM content_people;
DELETE FROM content_sports;
DELETE FROM content_videos;
DELETE FROM messages;
DELETE FROM notifications;
DELETE FROM conversations;
DELETE FROM social_accounts;
DELETE FROM follows;
DELETE FROM contents;
DELETE FROM tags;
DELETE FROM users;

SET FOREIGN_KEY_CHECKS = 1;
