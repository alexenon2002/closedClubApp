package ru.shim.closedclub.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultActions;
import ru.shim.closedclub.BaseIntegrationTest;
import ru.shim.closedclub.entity.QrCode;

import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class QrCodeControllerIntegrationTest extends BaseIntegrationTest {

    private static final String URL = "/api/v1/qr-codes";
    private static final long MISSING_ID = Long.MAX_VALUE;

    private static final UUID ORIGINAL_CODE =
            UUID.fromString("11111111-1111-4111-8111-111111111111");

    @Test
    @DisplayName("Проверяем создание и сохранение QR-кода для существующего участника.")
    @Sql("classpath:sql/member/active-member.sql")
    void create_whenMemberExists_returnsAndSavesQrCode() throws Exception {
        long memberId = 1L;

        ResultActions result = mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberId": %d}
                                """.formatted(memberId)))
                .andExpect(status().isOk());

        assertEquals(1L, qrCodeRepository.count());

        QrCode stored = qrCodeRepository.findAll().get(0);

        assertNotNull(stored.getCode());
        assertEquals(memberId, stored.getMember().getId());

        result.andExpect(jsonPath("$.id").value(stored.getId()))
                .andExpect(jsonPath("$.code").value(stored.getCode().toString()))
                .andExpect(jsonPath("$.memberId").value(memberId));
    }

    @Test
    @DisplayName("Проверяем ошибку 404 при создании QR-кода для несуществующего участника.")
    void create_whenMemberMissing_returnsNotFound() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberId": %d}
                                """.formatted(MISSING_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Участник с id " + MISSING_ID + " не найден"));

        assertEquals(0L, qrCodeRepository.count());
    }

    @Test
    @DisplayName("Проверяем ошибку 404 при создании QR-кода для удалённого участника.")
    @Sql("classpath:sql/member/deleted-member.sql")
    void create_whenMemberDeleted_returnsNotFound() throws Exception {
        long memberId = 1L;

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberId": %d}
                                """.formatted(memberId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Участник с id " + memberId + " не найден"));

        assertEquals(0L, qrCodeRepository.count());
    }

    @Test
    @DisplayName("Проверяем ошибку 400 при создании QR-кода без memberId.")
    void create_whenMemberIdMissing_returnsBadRequest() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value(startsWith("memberId: ")));

        assertEquals(0L, qrCodeRepository.count());
    }

    @Test
    @DisplayName("Проверяем получение существующего QR-кода по ID.")
    @Sql({
            "classpath:sql/member/active-member.sql",
            "classpath:sql/member/qr-code-for-member.sql"
    })
    void getById_whenQrCodeExists_returnsQrCode() throws Exception {
        long memberId = 1L;
        long qrCodeId = 1L;

        mockMvc.perform(get(URL + "/{id}", qrCodeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(qrCodeId))
                .andExpect(jsonPath("$.code").value(ORIGINAL_CODE.toString()))
                .andExpect(jsonPath("$.memberId").value(memberId));
    }

    @Test
    @DisplayName("Проверяем ошибку 404 при получении несуществующего QR-кода.")
    void getById_whenQrCodeMissing_returnsNotFound() throws Exception {
        mockMvc.perform(get(URL + "/{id}", MISSING_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("QR-код с id " + MISSING_ID + " не найден"));
    }

    @Test
    @DisplayName("Проверяем смену владельца QR-кода с сохранением прежнего UUID.")
    @Sql({
            "classpath:sql/qr/two-active-members.sql",
            "classpath:sql/member/qr-code-for-member.sql"
    })
    void update_whenMemberExists_changesOwnerAndKeepsCode() throws Exception {
        long qrCodeId = 1L;
        long newMemberId = 2L;

        mockMvc.perform(put(URL + "/{id}", qrCodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberId": %d}
                                """.formatted(newMemberId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(qrCodeId))
                .andExpect(jsonPath("$.code").value(ORIGINAL_CODE.toString()))
                .andExpect(jsonPath("$.memberId").value(newMemberId));

        QrCode stored = qrCodeRepository.findById(qrCodeId).orElseThrow();

        assertEquals(newMemberId, stored.getMember().getId());
        assertEquals(ORIGINAL_CODE, stored.getCode());
        assertEquals(1L, qrCodeRepository.count());
    }

    @Test
    @DisplayName("Проверяем ошибку 404 при обновлении несуществующего QR-кода.")
    @Sql("classpath:sql/member/active-member.sql")
    void update_whenQrCodeMissing_returnsNotFound() throws Exception {
        long memberId = 1L;

        mockMvc.perform(put(URL + "/{id}", MISSING_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberId": %d}
                                """.formatted(memberId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("QR-код с id " + MISSING_ID + " не найден"));

        assertEquals(0L, qrCodeRepository.count());
    }

    @Test
    @DisplayName("Проверяем, что назначение несуществующего владельца возвращает 404 и не меняет QR-код.")
    @Sql({
            "classpath:sql/member/active-member.sql",
            "classpath:sql/member/qr-code-for-member.sql"
    })
    void update_whenMemberMissing_returnsNotFoundAndKeepsQrCode() throws Exception {
        long memberId = 1L;
        long qrCodeId = 1L;

        mockMvc.perform(put(URL + "/{id}", qrCodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberId": %d}
                                """.formatted(MISSING_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Участник с id " + MISSING_ID + " не найден"));

        QrCode stored = qrCodeRepository.findById(qrCodeId).orElseThrow();

        assertEquals(memberId, stored.getMember().getId());
        assertEquals(ORIGINAL_CODE, stored.getCode());
    }

    @Test
    @DisplayName("Проверяем, что назначение удалённого владельца возвращает 404 и не меняет QR-код.")
    @Sql({
            "classpath:sql/member/active-member.sql",
            "classpath:sql/member/deleted-member.sql",
            "classpath:sql/member/qr-code-for-member.sql"
    })
    void update_whenMemberDeleted_returnsNotFoundAndKeepsQrCode() throws Exception {
        long originalMemberId = 1L;
        long deletedMemberId = 2L;
        long qrCodeId = 1L;

        mockMvc.perform(put(URL + "/{id}", qrCodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"memberId": %d}
                                """.formatted(deletedMemberId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Участник с id " + deletedMemberId + " не найден"));

        QrCode stored = qrCodeRepository.findById(qrCodeId).orElseThrow();

        assertEquals(originalMemberId, stored.getMember().getId());
        assertEquals(ORIGINAL_CODE, stored.getCode());
    }

    @Test
    @DisplayName("Проверяем, что обновление без memberId возвращает 400 и не меняет QR-код.")
    @Sql({
            "classpath:sql/member/active-member.sql",
            "classpath:sql/member/qr-code-for-member.sql"
    })
    void update_whenMemberIdMissing_returnsBadRequestAndKeepsQrCode() throws Exception {
        long memberId = 1L;
        long qrCodeId = 1L;

        mockMvc.perform(put(URL + "/{id}", qrCodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value(startsWith("memberId: ")));

        QrCode stored = qrCodeRepository.findById(qrCodeId).orElseThrow();

        assertEquals(memberId, stored.getMember().getId());
        assertEquals(ORIGINAL_CODE, stored.getCode());
    }

    @Test
    @DisplayName("Проверяем удаление QR-кода с сохранением его владельца в базе.")
    @Sql({
            "classpath:sql/member/active-member.sql",
            "classpath:sql/member/qr-code-for-member.sql"
    })
    void delete_whenQrCodeExists_removesQrCodeAndKeepsMember() throws Exception {
        long memberId = 1L;
        long qrCodeId = 1L;

        mockMvc.perform(delete(URL + "/{id}", qrCodeId))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        assertTrue(qrCodeRepository.findById(qrCodeId).isEmpty());
        assertTrue(
                memberRepository.findByIdAndDeletedAtIsNull(memberId).isPresent()
        );
    }

    @Test
    @DisplayName("Проверяем, что удаление несуществующего QR-кода возвращает 404 и сохраняет остальные записи.")
    @Sql({
            "classpath:sql/member/active-member.sql",
            "classpath:sql/member/qr-code-for-member.sql"
    })
    void delete_whenQrCodeMissing_returnsNotFoundAndKeepsExistingQrCode()
            throws Exception {
        long memberId = 1L;
        long qrCodeId = 1L;

        mockMvc.perform(delete(URL + "/{id}", MISSING_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("QR-код с id " + MISSING_ID + " не найден"));

        QrCode stored = qrCodeRepository.findById(qrCodeId).orElseThrow();

        assertEquals(ORIGINAL_CODE, stored.getCode());
        assertEquals(memberId, stored.getMember().getId());
    }

    @Test
    @DisplayName("Проверяем получение нужной страницы QR-кодов с сортировкой по ID.")
    @Sql({
            "classpath:sql/member/active-member.sql",
            "classpath:sql/qr/qr-codes-for-pagination.sql"
    })
    void getQrCodes_withoutFilter_returnsRequestedPage() throws Exception {
        mockMvc.perform(get(URL)
                        .param("page", "1")
                        .param("size", "1")
                        .param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(2))
                .andExpect(jsonPath("$.content[0].code")
                        .value("22222222-2222-4222-8222-222222222222"))
                .andExpect(jsonPath("$.content[0].memberId").value(1))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    @DisplayName("Проверяем, что фильтр memberId возвращает только QR-коды выбранного участника.")
    @Sql({
            "classpath:sql/qr/two-active-members.sql",
            "classpath:sql/qr/qr-codes-for-member-filter.sql"
    })
    void getQrCodes_withMemberId_returnsOnlyTheirQrCodes() throws Exception {
        mockMvc.perform(get(URL)
                        .param("memberId", "1")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[1].id").value(3))
                .andExpect(jsonPath("$.content[0].memberId").value(1))
                .andExpect(jsonPath("$.content[1].memberId").value(1))
                .andExpect(jsonPath("$.totalElements").value(2));
    }
}