INSERT INTO creature(id, ac, alignment, cost, fort, hp, level, movementMode, name, ref, speed, will, keywords, factions, championRating) VALUES (1, 20, 'NEUTRAL', 20, 16, 17, 5, 'NORMAL', 'Rask 1', 19, 8, 20, 'Half-orc, Martial', 'CIVILIZATION', 2);
INSERT INTO creature(id, ac, alignment, cost, fort, hp, level, movementMode, name, ref, speed, will, factions, championRating) VALUES (2, 20, 'NEUTRAL', 20, 16, 17, 5, 'NORMAL', 'Warforged scout 1', 19, 8, 20, 'CIVILIZATION', 0);
INSERT INTO creature(id, ac, alignment, cost, fort, hp, level, movementMode, name, ref, speed, will, factions, championRating) VALUES (3, 20, 'NEUTRAL', 20, 16, 17, 5, 'NORMAL', 'Sacred watcher 1', 19, 8, 20, 'WILD:CIVILIZATION', 0);

/*INSERT INTO creature_factions (creature_id, faction) VALUES (1, 'CIVILIZATION');
INSERT INTO creature_factions (creature_id, faction) VALUES (1, 'WILD');
INSERT INTO creature_factions (creature_id, faction) VALUES (2, 'CIVILIZATION');
INSERT INTO creature_factions (creature_id, faction) VALUES (2, 'BORDERLANDS');
INSERT INTO creature_factions (creature_id, faction) VALUES (3, 'CIVILIZATION');*/

INSERT INTO app_user(id, username, password, roles, active) VALUES (1, 'mkouba', '$2a$10$dd20n4sEkb6CmrLUU9AhIeqRgAwZsjuZfJaqCQHoS48bt5pDsIRX.', 'USER:ADMIN', true);
INSERT INTO app_user(id, username, password, roles, active) VALUES (2, 'foo', '$2a$10$dd20n4sEkb6CmrLUU9AhIeqRgAwZsjuZfJaqCQHoS48bt5pDsIRX.', 'USER', true);

INSERT INTO warband(id, name, point_limit, arena, user_id, public_link, freestyle) VALUES (1, 'Testik', 'STANDARD', true, 1, true, false);

INSERT INTO warband_creature(id, warband_id, creature_id) VALUES (1, 1, 1);
INSERT INTO warband_creature(id, warband_id, creature_id) VALUES (2, 1, 2);
INSERT INTO warband_creature(id, warband_id, creature_id) VALUES (3, 1, 3);

INSERT INTO user_creature(id, user_id, creature_id) VALUES (1, 1, 1);
INSERT INTO user_creature(id, user_id, creature_id) VALUES (2, 2, 1);

ALTER SEQUENCE hibernate_sequence RESTART WITH 100; 
