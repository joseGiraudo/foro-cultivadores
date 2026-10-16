-- Campos de perfil de comercio/growshop (opcionales, nullable). Solo los completan
-- usuarios con rol suficiente (forum.rules.business-profile-min-role, CONTRIBUTOR por defecto).
ALTER TABLE users
    ADD COLUMN website   VARCHAR(255),
    ADD COLUMN phone     VARCHAR(30),
    ADD COLUMN instagram VARCHAR(50),
    ADD COLUMN location  VARCHAR(100);
