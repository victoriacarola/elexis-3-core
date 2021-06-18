DELETE FROM REMINDERS_RESPONSIBLE_LINK
WHERE id IN (SELECT id
              FROM (SELECT id,
                             ROW_NUMBER() OVER (partition BY ReminderID, ResponsibleID ORDER BY id) AS rnum
                     FROM REMINDERS_RESPONSIBLE_LINK) t
              WHERE t.rnum > 1);

ALTER TABLE REMINDERS_RESPONSIBLE_LINK DROP CONSTRAINT IF EXISTS reminders_responsible_link_pkey;
ALTER TABLE REMINDERS_RESPONSIBLE_LINK
    ALTER COLUMN id TYPE character varying ;
ALTER TABLE REMINDERS_RESPONSIBLE_LINK
    ALTER COLUMN id DROP NOT NULL;
ALTER TABLE REMINDERS_RESPONSIBLE_LINK DROP CONSTRAINT IF EXISTS primary_key;
ALTER TABLE REMINDERS_RESPONSIBLE_LINK
    ADD CONSTRAINT reminders_responsible_link_pkey PRIMARY KEY (reminderid, responsibleid);
    
ALTER TABLE REMINDERS ADD ExtInfo BYTEA;
