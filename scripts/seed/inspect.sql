BEGIN READ ONLY;
SELECT column_name, data_type, character_maximum_length, is_nullable, column_default
FROM information_schema.columns WHERE table_schema=current_schema() AND table_name='emotion_map_markers' ORDER BY ordinal_position;
SELECT conname, pg_get_constraintdef(oid) AS definition FROM pg_constraint
WHERE conrelid IN ('users'::regclass,'emotion_map_markers'::regclass,'emotion_map_marker_tags'::regclass) ORDER BY conrelid,conname;
SELECT emotion_label, emotion_color, count(*) FROM emotion_map_markers GROUP BY 1,2 ORDER BY 1;
SELECT count(*) AS all_users FROM users;
SELECT count(*) AS all_records FROM emotion_map_markers;
SELECT count(*) AS seed_records FROM emotion_map_markers m JOIN users u ON u.id=m.user_id WHERE left(u.username,17)='seed_seunghak_v1_';
COMMIT;
