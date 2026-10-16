package ru.shim.closedclub.controller;

import org.springframework.test.context.jdbc.Sql;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import ru.shim.closedclub.BaseIntegrationTest;
import ru.shim.closedclub.entity.Member;
import ru.shim.closedclub.entity.QrCode;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MemberControllerIntegrationTest extends BaseIntegrationTest {

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


    @DisplayName("Создание участника: проверяем ответ и сохранение в БД.")
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

    @DisplayName("Пустые имя и фамилия: получаем 400, запись не создаётся.")
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

    @DisplayName("Получение существующего участника.")
    @Test
    @Sql("classpath:sql/member/active-member.sql")
    void getById_whenMemberExists_returnsMember() throws Exception {

        long memberId = 1L;

        mockMvc.perform(get(URL + "/{id}", memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(memberId))
                .andExpect(jsonPath("$.firstName").value("Иван"))
                .andExpect(jsonPath("$.middleName").value("Петрович"))
                .andExpect(jsonPath("$.surname").value("Петров"));
    }

    @DisplayName("Получение участника, которого нет в БД.")
    @Test
    void getById_whenMemberMissing_returnsNotFound() throws Exception {
        mockMvc.perform(get(URL + "/{id}", MISSING_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Участник с id " + MISSING_ID + " не найден"));
    }

    @DisplayName("Удалённый участник не должен возвращаться через API.")
    @Test
    @Sql("classpath:sql/member/deleted-member.sql")
    void getById_whenMemberDeleted_returnsNotFound() throws Exception {
        long memberId = 1L;

        Member stored = memberRepository.findById(memberId).orElseThrow();

        assertNotNull(stored.getDeletedAt());

        mockMvc.perform(get(URL + "/{id}", memberId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Участник с id " + memberId + " не найден"));
    }

    @DisplayName("Пагинация списка: удалённые участники не учитываются.")
    @Test
    @Sql("classpath:sql/member/members-for-pagination.sql")
    void getMembers_withoutSurname_returnsPageAndExcludesDeleted() throws Exception {
        long secondActiveMemberId = 3L;

        mockMvc.perform(
                        get(URL)
                                .param("page", "1")
                                .param("size", "1")
                                .param("sort", "id,asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(secondActiveMemberId))
                .andExpect(jsonPath("$.content[0].surname").value("Петров"))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @DisplayName("Поиск по части фамилии без учёта регистра.")
    @Test
    @Sql("classpath:sql/member/members-for-surname-filter.sql")
    void getMembers_withSurname_filtersIgnoringCaseAndExcludesDeleted() throws Exception {
        long matchingMemberId = 1L;

        mockMvc.perform(
                        get(URL)
                                .param("surname", "еТр")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(matchingMemberId))
                .andExpect(jsonPath("$.content[0].surname").value("Петров"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @DisplayName("Фильтр из пробелов должен работать как отсутствие фильтра.")
    @Test
    @Sql("classpath:sql/member/members-for-blank-filter.sql")
    void getMembers_withBlankSurname_returnsAllActiveMembers() throws Exception {
        long firstMemberId = 1L;
        long secondMemberId = 2L;

        mockMvc.perform(
                        get(URL)
                                .param("surname", "   ")
                                .param("sort", "id,asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(firstMemberId))
                .andExpect(jsonPath("$.content[1].id").value(secondMemberId))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @DisplayName("Обновление данных, включая удаление необязательного отчества.")
    @Test
    @Sql("classpath:sql/member/active-member.sql")
    void update_whenRequestValid_persistsChangesAndClearsMiddleName() throws Exception {
        long memberId = 1L;

        mockMvc.perform(
                        put(URL + "/{id}", memberId)
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
                .andExpect(jsonPath("$.id").value(memberId))
                .andExpect(jsonPath("$.firstName").value("Пётр"))
                .andExpect(jsonPath("$.middleName").value(nullValue()))
                .andExpect(jsonPath("$.surname").value("Сидоров"));

        Member stored = memberRepository
                .findById(memberId)
                .orElseThrow();

        assertEquals("Пётр", stored.getFirstName());
        assertNull(stored.getMiddleName());
        assertEquals("Сидоров", stored.getSurname());
        assertNull(stored.getDeletedAt());
        assertEquals(1L, memberRepository.count());
    }

    @DisplayName("Некорректное обновление не должно менять сохранённые данные.")
    @Test
    @Sql("classpath:sql/member/active-member.sql")
    void update_whenRequiredFieldsBlank_returnsBadRequestAndKeepsMember() throws Exception {
        long memberId = 1L;

        mockMvc.perform(
                        put(URL + "/{id}", memberId)
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
                .findById(memberId)
                .orElseThrow();

        assertEquals("Иван", stored.getFirstName());
        assertEquals("Петрович", stored.getMiddleName());
        assertEquals("Петров", stored.getSurname());
        assertNull(stored.getDeletedAt());
        assertEquals(1L, memberRepository.count());
    }

    @DisplayName("Обновление отсутствующего участника.")
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

    @DisplayName("Обновление удалённого участника должно быть запрещено через 404.")
    @Test
    @Sql("classpath:sql/member/deleted-member.sql")
    void update_whenMemberDeleted_returnsNotFoundAndKeepsMember() throws Exception {
        long memberId = 1L;

        mockMvc.perform(
                        put(URL + "/{id}", memberId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_REQUEST)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        Member stored = memberRepository
                .findById(memberId)
                .orElseThrow();

        assertEquals("Иван", stored.getFirstName());
        assertEquals("Петрович", stored.getMiddleName());
        assertEquals("Сидоров", stored.getSurname());
        assertNotNull(stored.getDeletedAt());
        assertEquals(1L, memberRepository.count());
    }

    @DisplayName("soft delete сохраняет запись участника и его QR-код.")
    @Test
    @Sql({
            "classpath:sql/member/active-member.sql",
            "classpath:sql/member/qr-code-for-member.sql"
    })
    void delete_whenMemberExists_marksDeletedKeepsQrAndRejectsRepeat() throws Exception {
        long memberId = 1L;
        long qrCodeId = 1L;

        UUID originalCode = UUID.fromString("11111111-1111-4111-8111-111111111111");

        mockMvc.perform(delete(URL + "/{id}", memberId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        Member stored = memberRepository
                .findById(memberId)
                .orElseThrow();

        assertNotNull(stored.getDeletedAt());
        assertEquals("Петров", stored.getSurname());
        assertEquals(1L, memberRepository.count());

        assertTrue(memberRepository
                .findByIdAndDeletedAtIsNull(memberId)
                .isEmpty());

        QrCode storedQrCode = qrCodeRepository
                .findById(qrCodeId)
                .orElseThrow();

        assertEquals(originalCode, storedQrCode.getCode());
        assertEquals(memberId, storedQrCode.getMember().getId());
        assertEquals(1L, qrCodeRepository.count());

        // Повторное удаление возвращает 404.
        mockMvc.perform(delete(URL + "/{id}", memberId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        Member afterRepeatedDelete = memberRepository
                .findById(memberId)
                .orElseThrow();

        assertEquals(
                stored.getDeletedAt(),
                afterRepeatedDelete.getDeletedAt()
        );
    }

    @DisplayName("Удаление отсутствующего участника.")
    @Test
    void delete_whenMemberMissing_returnsNotFound() throws Exception {
        mockMvc.perform(delete(URL + "/{id}", MISSING_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        assertEquals(0L, memberRepository.count());
    }
}