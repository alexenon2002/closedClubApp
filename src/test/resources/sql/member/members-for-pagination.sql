-- id = 1: удалённый участник
INSERT INTO public.members (first_name, middle_name, surname, created_at, updated_at, deleted_at)
VALUES ('Иван', 'Петрович', 'Смирнов',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- id = 2: первый активный участник
INSERT INTO public.members (first_name, middle_name, surname, created_at, updated_at, deleted_at)
VALUES ('Иван', 'Петрович', 'Антонов',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL);

-- id = 3: второй активный участник
INSERT INTO public.members (first_name, middle_name, surname, created_at, updated_at, deleted_at)
VALUES ('Иван', 'Петрович', 'Петров',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL);