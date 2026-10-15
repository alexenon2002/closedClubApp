-- id = 1: активный участник
INSERT INTO public.members (first_name, middle_name, surname, created_at, updated_at, deleted_at)
VALUES ('Иван', 'Петрович', 'Петров',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL);

-- id = 2: активный участник
INSERT INTO public.members (first_name, middle_name, surname, created_at, updated_at, deleted_at)
VALUES ('Иван', 'Петрович', 'Иванов',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL);

-- id = 3: удалённый участник
INSERT INTO public.members (first_name, middle_name, surname, created_at, updated_at, deleted_at)
VALUES ('Иван', 'Петрович', 'Сидоров',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);