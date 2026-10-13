package ru.shim.closedclub.controller;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ru.shim.closedclub.config.PostgresTestConfiguration;
import ru.shim.closedclub.entity.Member;
import ru.shim.closedclub.entity.QrCode;
import ru.shim.closedclub.repository.MemberRepository;
import ru.shim.closedclub.repository.QrCodeRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class MemberControllerIntegrationTest {

    private static final String URL = "/api/v1/members";
    private static final long MISSING_ID = Long.MAX_VALUE;

    private static final String VALID_REQUEST = """
            {
                "firstName": "Иван",
                "middleName": "Петрович",
                "surname": "Петров"
            }
            """;

    private static final String INVALID_REQUEST = """
            {
                "firstName": " ",
                "middleName": "Петрович",
                "surname": " "
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private QrCodeRepository qrCodeRepository;

    @BeforeEach
    void clearDatabase() {
        qrCodeRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    // Создание участника: проверяем ответ и сохранение в БД.
    @Test
    void create_whenRequestValid_returnsAndPersistsMember() throws Exception {
        ResultActions result = mockMvc.perform(
                        post(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_REQUEST)
                )
                .andExpect(status().isOk());

        List<Member> members = memberRepository.findAll();
        assertEquals(1, members.size());

        Member stored = members.get(0);

        assertEquals("Иван", stored.getFirstName());
        assertEquals("Петрович", stored.getMiddleName());
        assertEquals("Петров", stored.getSurname());
        assertNull(stored.getDeletedAt());

        result.andExpect(jsonPath("$.id").value(stored.getId()))
                .andExpect(jsonPath("$.firstName").value("Иван"))
                .andExpect(jsonPath("$.middleName").value("Петрович"))
                .andExpect(jsonPath("$.surname").value("Петров"));
    }

    // Пустые имя и фамилия: получаем 400, запись не создаётся.
    @Test
    void create_whenRequiredFieldsBlank_returnsBadRequest() throws Exception {
        mockMvc.perform(
                        post(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(INVALID_REQUEST)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value(containsString("firstName")))
                .andExpect(jsonPath("$.message")
                        .value(containsString("surname")));

        assertEquals(0L, memberRepository.count());
    }

    // Получение существующего участника.
    @Test
    void getById_whenMemberExists_returnsMember() throws Exception {
        Member member = saveMember("Петров");

        mockMvc.perform(get(URL + "/{id}", member.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(member.getId()))
                .andExpect(jsonPath("$.firstName").value("Иван"))
                .andExpect(jsonPath("$.middleName").value("Петрович"))
                .andExpect(jsonPath("$.surname").value("Петров"));
    }

    // Получение участника, которого нет в БД.
    @Test
    void getById_whenMemberMissing_returnsNotFound() throws Exception {
        mockMvc.perform(get(URL + "/{id}", MISSING_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Участник с id " + MISSING_ID + " не найден"));
    }

    // Удалённый участник не должен возвращаться через API.
    @Test
    void getById_whenMemberDeleted_returnsNotFound() throws Exception {
        Member member = saveDeletedMember("Петров");

        mockMvc.perform(get(URL + "/{id}", member.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Участник с id " + member.getId() + " не найден"));
    }

    // Пагинация списка: удалённые участники не учитываются.
    @Test
    void getMembers_withoutSurname_returnsPageAndExcludesDeleted() throws Exception {
        saveDeletedMember("Смирнов");
        saveMember("Антонов");
        Member second = saveMember("Петров");

        mockMvc.perform(
                        get(URL)
                                .param("page", "1")
                                .param("size", "1")
                                .param("sort", "id,asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(second.getId()))
                .andExpect(jsonPath("$.content[0].surname").value("Петров"))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    // Поиск по части фамилии без учёта регистра.
    @Test
    void getMembers_withSurname_filtersIgnoringCaseAndExcludesDeleted() throws Exception {
        Member matching = saveMember("Петров");
        saveMember("Иванов");
        saveDeletedMember("ПЕТРЕНКО");

        mockMvc.perform(
                        get(URL)
                                .param("surname", "еТр")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(matching.getId()))
                .andExpect(jsonPath("$.content[0].surname").value("Петров"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // Фильтр из пробелов должен работать как отсутствие фильтра.
    @Test
    void getMembers_withBlankSurname_returnsAllActiveMembers() throws Exception {
        Member first = saveMember("Петров");
        Member second = saveMember("Иванов");
        saveDeletedMember("Сидоров");

        mockMvc.perform(
                        get(URL)
                                .param("surname", "   ")
                                .param("sort", "id,asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(first.getId()))
                .andExpect(jsonPath("$.content[1].id").value(second.getId()))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    // Обновление данных, включая удаление необязательного отчества.
    @Test
    void update_whenRequestValid_persistsChangesAndClearsMiddleName() throws Exception {
        Member member = saveMember("Петров");

        mockMvc.perform(
                        put(URL + "/{id}", member.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                            "firstName": "Пётр",
                                            "middleName": null,
                                            "surname": "Сидоров"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(member.getId()))
                .andExpect(jsonPath("$.firstName").value("Пётр"))
                .andExpect(jsonPath("$.middleName").value(nullValue()))
                .andExpect(jsonPath("$.surname").value("Сидоров"));

        Member stored = memberRepository
                .findById(member.getId())
                .orElseThrow();

        assertEquals("Пётр", stored.getFirstName());
        assertNull(stored.getMiddleName());
        assertEquals("Сидоров", stored.getSurname());
        assertNull(stored.getDeletedAt());
        assertEquals(1L, memberRepository.count());
    }

    // Некорректное обновление не должно менять сохранённые данные.
    @Test
    void update_whenRequiredFieldsBlank_returnsBadRequestAndKeepsMember() throws Exception {
        Member member = saveMember("Петров");

        mockMvc.perform(
                        put(URL + "/{id}", member.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(INVALID_REQUEST)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value(containsString("firstName")))
                .andExpect(jsonPath("$.message")
                        .value(containsString("surname")));

        Member stored = memberRepository
                .findById(member.getId())
                .orElseThrow();

        assertEquals("Иван", stored.getFirstName());
        assertEquals("Петрович", stored.getMiddleName());
        assertEquals("Петров", stored.getSurname());
        assertNull(stored.getDeletedAt());
        assertEquals(1L, memberRepository.count());
    }

    // Обновление отсутствующего участника.
    @Test
    void update_whenMemberMissing_returnsNotFound() throws Exception {
        mockMvc.perform(
                        put(URL + "/{id}", MISSING_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_REQUEST)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        assertEquals(0L, memberRepository.count());
    }

    // Обновление удалённого участника должно быть запрещено через 404.
    @Test
    void update_whenMemberDeleted_returnsNotFoundAndKeepsMember() throws Exception {
        Member member = saveDeletedMember("Сидоров");

        mockMvc.perform(
                        put(URL + "/{id}", member.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_REQUEST)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        Member stored = memberRepository
                .findById(member.getId())
                .orElseThrow();

        assertEquals("Иван", stored.getFirstName());
        assertEquals("Петрович", stored.getMiddleName());
        assertEquals("Сидоров", stored.getSurname());
        assertNotNull(stored.getDeletedAt());
        assertEquals(1L, memberRepository.count());
    }

    // soft delete сохраняет запись участника и его QR-код.
    @Test
    void delete_whenMemberExists_marksDeletedKeepsQrAndRejectsRepeat() throws Exception {
        Member member = saveMember("Петров");

        UUID originalCode = UUID.randomUUID();

        QrCode qrCode = new QrCode();
        qrCode.setCode(originalCode);
        qrCode.setMember(member);

        QrCode savedQrCode = qrCodeRepository.saveAndFlush(qrCode);

        mockMvc.perform(delete(URL + "/{id}", member.getId()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        Member stored = memberRepository
                .findById(member.getId())
                .orElseThrow();

        assertNotNull(stored.getDeletedAt());
        assertEquals("Петров", stored.getSurname());
        assertEquals(1L, memberRepository.count());

        assertTrue(memberRepository
                .findByIdAndDeletedAtIsNull(member.getId())
                .isEmpty());

        QrCode storedQrCode = qrCodeRepository
                .findById(savedQrCode.getId())
                .orElseThrow();

        assertEquals(originalCode, storedQrCode.getCode());
        assertEquals(member.getId(), storedQrCode.getMember().getId());
        assertEquals(1L, qrCodeRepository.count());

        // Повторное удаление возвращает 404.
        mockMvc.perform(delete(URL + "/{id}", member.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        Member afterRepeatedDelete = memberRepository
                .findById(member.getId())
                .orElseThrow();

        assertEquals(
                stored.getDeletedAt(),
                afterRepeatedDelete.getDeletedAt()
        );
    }

    // Удаление отсутствующего участника.
    @Test
    void delete_whenMemberMissing_returnsNotFound() throws Exception {
        mockMvc.perform(delete(URL + "/{id}", MISSING_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        assertEquals(0L, memberRepository.count());
    }

    // Вспомогательный метод подготовки активного участника.
    private @NonNull Member saveMember(String surname) {
        Member member = new Member();
        member.setFirstName("Иван");
        member.setMiddleName("Петрович");
        member.setSurname(surname);

        return memberRepository.saveAndFlush(member);
    }

    // Вспомогательный метод подготовки удалённого участника.
    private @NonNull Member saveDeletedMember(String surname) {
        Member member = saveMember(surname);
        member.setDeletedAt(LocalDateTime.now().minusDays(1));

        return memberRepository.saveAndFlush(member);
    }
}