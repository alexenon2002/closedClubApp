INSERT INTO public.members (first_name,
                            middle_name,
                            surname,
                            created_at,
                            updated_at,
                            deleted_at)
VALUES ('Иван',
        'Петрович',
        'Петров',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        NULL);

INSERT INTO public.qr_codes (code,
                             member_id,
                             created_at,
                             updated_at,
                             deleted_at)
VALUES ('11111111-1111-4111-8111-111111111111',
        1,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        NULL);