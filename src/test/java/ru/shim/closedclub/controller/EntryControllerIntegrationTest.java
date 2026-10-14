package ru.shim.closedclub.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import ru.shim.closedclub.BaseIntegrationTest;
import ru.shim.closedclub.entity.Member;
import ru.shim.closedclub.entity.QrCode;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;


class EntryControllerIntegrationTest extends BaseIntegrationTest {

    private static final String URL = "/api/v1/entry";


    @DisplayName("Проверяем успешный вход и замену использованного QR-кода новым.")
    @Test
    void enter_whenCodeExists_returnsMemberAndRotatesCode() throws Exception {
        Member member = new Member();
        member.setFirstName("Иван");
        member.setMiddleName("Петрович");
        member.setSurname("Петров");
        Member savedMember = memberRepository.saveAndFlush(member);

        UUID originalCode = UUID.randomUUID();
        QrCode qrCode = new QrCode();
        qrCode.setCode(originalCode);
        qrCode.setMember(savedMember);
        QrCode savedQrCode = qrCodeRepository.saveAndFlush(qrCode);

        ResultActions result = mockMvc.perform(
                post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "%s"}
                                """.formatted(originalCode)));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(savedMember.getId()))
                .andExpect(jsonPath("$.fullName").value("Иван Петрович Петров"));

        QrCode storedQrCode = qrCodeRepository.findById(savedQrCode.getId())
                .orElseThrow();

        assertNotNull(storedQrCode.getCode());
        assertNotEquals(originalCode, storedQrCode.getCode());
        assertTrue(qrCodeRepository.findByCode(originalCode).isEmpty());

        result.andExpect(jsonPath("$.newQrCode").value(storedQrCode.getCode().toString()));
    }

    @DisplayName("Проверяем, что повторный вход по использованному QR-коду запрещён.")
    @Test
    void enter_whenCodeAlreadyUsed_returnsForbidden() throws Exception {
        Member member = new Member();
        member.setFirstName("Иван");
        member.setMiddleName("Петрович");
        member.setSurname("Петров");
        Member savedMember = memberRepository.saveAndFlush(member);

        UUID originalCode = UUID.randomUUID();
        QrCode qrCode = new QrCode();
        qrCode.setCode(originalCode);
        qrCode.setMember(savedMember);
        QrCode savedQrCode = qrCodeRepository.saveAndFlush(qrCode);

        ResultActions result = mockMvc.perform(
                post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "%s"}
                                """.formatted(originalCode)));


        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(savedMember.getId()))
                .andExpect(jsonPath("$.fullName").value("Иван Петрович Петров"));

        UUID codeAfterFirstEntry = qrCodeRepository
                .findById(savedQrCode.getId())
                .orElseThrow()
                .getCode();

        mockMvc.perform(
                        post(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"code": "%s"}
                                        """.formatted(originalCode))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message")
                        .value("Вход запрещён: QR-код не найден"));

        QrCode storedQrCode = qrCodeRepository
                .findById(savedQrCode.getId())
                .orElseThrow();

        assertEquals(codeAfterFirstEntry, storedQrCode.getCode());
    }

    @DisplayName("Проверяем, что удалённому участнику запрещён вход и его QR-код не меняется.")
    @Test
    void enter_whenMemberDeleted_returnsForbiddenAndKeepsCode() throws Exception {
        Member member = new Member();
        member.setFirstName("Иван");
        member.setMiddleName("Петрович");
        member.setSurname("Петров");
        member.setDeletedAt(LocalDateTime.now());

        Member savedMember = memberRepository.saveAndFlush(member);

        UUID originalCode = UUID.randomUUID();

        QrCode qrCode = new QrCode();
        qrCode.setCode(originalCode);
        qrCode.setMember(savedMember);

        QrCode savedQrCode = qrCodeRepository.saveAndFlush(qrCode);

        mockMvc.perform(
                        post(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"code": "%s"}
                                        """.formatted(originalCode))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message")
                        .value("Вход запрещён: участник удалён"));

        QrCode storedQrCode = qrCodeRepository
                .findById(savedQrCode.getId())
                .orElseThrow();

        assertEquals(originalCode, storedQrCode.getCode());
    }

    @DisplayName("Проверяем ошибку 400 при отсутствии обязательного поля code.")
    @Test
    void enter_whenCodeMissing_returnsBadRequest() throws Exception {
        mockMvc.perform(
                        post(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value(startsWith("code: ")));
    }

    @DisplayName("Проверяем ошибку 400 при неверном формате UUID.")
    @Test
    void enter_whenCodeHasInvalidFormat_returnsBadRequest() throws Exception {
        mockMvc.perform(
                post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code": "not-a-uuid"}
                                """)
        ).andExpect(status().isBadRequest());
    }

    @DisplayName("Проверяем ошибку 403 при входе по несуществующему QR-коду.")
    @Test
    void enter_whenCodeNotFound_returnsForbidden() throws Exception {
        UUID unknownQrCode = UUID.randomUUID();

        mockMvc.perform(
                        post(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"code": "%s"}
                                        """.formatted(unknownQrCode))
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message")
                        .value("Вход запрещён: QR-код не найден"));
    }
}
