--liquibase formatted sql

--changeset alexenon2002:002-qr-code-timestamps dbms:postgresql logicalFilePath:db/changelog/changes/002-qr-code-timestamps.sql

ALTER TABLE public.qr_codes
    ADD COLUMN created_at TIMESTAMP(6) WITHOUT TIME ZONE,
    ADD COLUMN updated_at TIMESTAMP(6) WITHOUT TIME ZONE,
    ADD COLUMN deleted_at TIMESTAMP(6) WITHOUT TIME ZONE;

UPDATE public.qr_codes
SET created_at = LOCALTIMESTAMP(6),
    updated_at = LOCALTIMESTAMP(6);

ALTER TABLE public.qr_codes
ALTER COLUMN created_at SET NOT NULL,
ALTER COLUMN updated_at SET NOT NULL;