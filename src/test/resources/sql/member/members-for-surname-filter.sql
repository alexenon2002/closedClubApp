-- id = 1: подходит под поиск и является активным
INSERT INTO public.members (first_name, middle_name, surname, created_at, updated_at, deleted_at)
VALUES ('Иван', 'Петрович', 'Петров',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL);

-- id = 2: активный, но не подходит под поиск
INSERT INTO public.members (first_name, middle_name, surname, created_at, updated_at, deleted_at)
VALUES ('Иван', 'Петрович', 'Иванов',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL);

-- id = 3: подходит под поиск, но удалён
INSERT INTO public.members (first_name, middle_name, surname, created_at, updated_at, deleted_at)
VALUES ('Иван', 'Петрович', 'ПЕТРЕНКО',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);