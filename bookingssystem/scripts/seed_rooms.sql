-- Seeds a few rooms into the (empty) Supabase database.
-- Run once in the Supabase SQL editor after the backend has (re)created the schema
-- (Hibernate ddl-auto=update creates the "room" table on startup).
-- Room names match react_frontend/src/app/lib/roomImages.ts exactly, so they render
-- with the real uploaded photos instead of the default placeholder.

insert into room (id, name, capacity, location, price_per_night, room_status) values
  (gen_random_uuid(), 'Konferenz-Raum',         12, 'Hauptgebäude, 2. OG', 45.00, 'VERFUGBAR'),
  (gen_random_uuid(), 'Meetingraum Beta',         6, 'Hauptgebäude, 1. OG', 25.00, 'VERFUGBAR'),
  (gen_random_uuid(), 'Vorlesungssaal Epsilon',  120, 'Gebäude C, EG',      150.00, 'VERFUGBAR'),
  (gen_random_uuid(), 'Party-Raum',               80, 'Nebengebäude',      200.00, 'VERFUGBAR'),
  (gen_random_uuid(), 'Mathe Raum',               30, 'Gebäude B, 3. OG',   60.00, 'VERFUGBAR'),
  (gen_random_uuid(), 'Room1',                     4, 'Hauptgebäude, EG',   15.00, 'GEBUCHT');
