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
import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestConfiguration.class)
class QrCodeControllerIntegrationTest {

    private static final String URL = "/api/v1/qr-codes";
    private static final long MISSING_ID = Long.MAX_VALUE;

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

    // Проверяем создание и сохранение QR-кода для существующего участника.
    @Test
    void create_whenMemberExists_returnsAndSavesQrCode() throws Exception {
        Member member = saveMember();

        ResultActions result = mockMvc.perform(
                        post(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"memberId": %d}
                                        """.formatted(member.getId()))
                )
                .andExpect(status().isOk());

        assertEquals(1L, qrCodeRepository.count());

        QrCode stored = qrCodeRepository.findAll().get(0);

        assertNotNull(stored.getCode());
        assertEquals(member.getId(), stored.getMember().getId());

        result.andExpect(jsonPath("$.id").value(stored.getId()))
                .andExpect(jsonPath("$.code")
                        .value(stored.getCode().toString()))
                .andExpect(jsonPath("$.memberId").value(member.getId()));
    }

    // Проверяем ошибку 404 при создании QR-кода для несуществующего участника.
    @Test
    void create_whenMemberMissing_returnsNotFound() throws Exception {
        mockMvc.perform(
                        post(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"memberId": %d}
                                        """.formatted(MISSING_ID))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Участник с id " + MISSING_ID + " не найден"));

        assertEquals(0L, qrCodeRepository.count());
    }

    // Проверяем ошибку 404 при создании QR-кода для удалённого участника.
    @Test
    void create_whenMemberDeleted_returnsNotFound() throws Exception {
        Member member = saveMember();
        member.setDeletedAt(LocalDateTime.now());
        memberRepository.saveAndFlush(member);

        mockMvc.perform(
                        post(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"memberId": %d}
                                        """.formatted(member.getId()))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Участник с id " + member.getId() + " не найден"));

        assertEquals(0L, qrCodeRepository.count());
    }

    // Проверяем ошибку 400 при создании QR-кода без memberId.
    @Test
    void create_whenMemberIdMissing_returnsBadRequest() throws Exception {
        mockMvc.perform(
                        post(URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value(startsWith("memberId: ")));

        assertEquals(0L, qrCodeRepository.count());
    }

    // Проверяем получение существующего QR-кода по ID.
    @Test
    void getById_whenQrCodeExists_returnsQrCode() throws Exception {
        Member member = saveMember();
        QrCode qrCode = saveQrCode(member);

        mockMvc.perform(get(URL + "/{id}", qrCode.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(qrCode.getId()))
                .andExpect(jsonPath("$.code")
                        .value(qrCode.getCode().toString()))
                .andExpect(jsonPath("$.memberId").value(member.getId()));
    }

    // Проверяем ошибку 404 при получении несуществующего QR-кода.
    @Test
    void getById_whenQrCodeMissing_returnsNotFound() throws Exception {
        mockMvc.perform(get(URL + "/{id}", MISSING_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("QR-код с id " + MISSING_ID + " не найден"));
    }

    // Проверяем смену владельца QR-кода с сохранением прежнего UUID.
    @Test
    void update_whenMemberExists_changesOwnerAndKeepsCode() throws Exception {
        Member originalMember = saveMember();
        Member newMember = saveMember();

        QrCode qrCode = saveQrCode(originalMember);
        UUID originalCode = qrCode.getCode();

        mockMvc.perform(
                        put(URL + "/{id}", qrCode.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"memberId": %d}
                                        """.formatted(newMember.getId()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(qrCode.getId()))
                .andExpect(jsonPath("$.code").value(originalCode.toString()))
                .andExpect(jsonPath("$.memberId").value(newMember.getId()));

        QrCode stored = qrCodeRepository
                .findById(qrCode.getId())
                .orElseThrow();

        assertEquals(newMember.getId(), stored.getMember().getId());
        assertEquals(originalCode, stored.getCode());
        assertEquals(1L, qrCodeRepository.count());
    }

    // Проверяем ошибку 404 при обновлении несуществующего QR-кода.
    @Test
    void update_whenQrCodeMissing_returnsNotFound() throws Exception {
        Member member = saveMember();

        mockMvc.perform(
                        put(URL + "/{id}", MISSING_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"memberId": %d}
                                        """.formatted(member.getId()))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("QR-код с id " + MISSING_ID + " не найден"));

        assertEquals(0L, qrCodeRepository.count());
    }

    // Проверяем, что назначение несуществующего владельца возвращает 404 и не меняет QR-код.
    @Test
    void update_whenMemberMissing_returnsNotFoundAndKeepsQrCode() throws Exception {
        Member member = saveMember();
        QrCode qrCode = saveQrCode(member);

        mockMvc.perform(
                        put(URL + "/{id}", qrCode.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"memberId": %d}
                                        """.formatted(MISSING_ID))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Участник с id " + MISSING_ID + " не найден"));

        QrCode stored = qrCodeRepository
                .findById(qrCode.getId())
                .orElseThrow();

        assertEquals(member.getId(), stored.getMember().getId());
        assertEquals(qrCode.getCode(), stored.getCode());
    }

    // Проверяем, что назначение удалённого владельца возвращает 404 и не меняет QR-код.
    @Test
    void update_whenMemberDeleted_returnsNotFoundAndKeepsQrCode() throws Exception {
        Member originalMember = saveMember();
        QrCode qrCode = saveQrCode(originalMember);

        Member deletedMember = saveMember();
        deletedMember.setDeletedAt(LocalDateTime.now());
        memberRepository.saveAndFlush(deletedMember);

        mockMvc.perform(
                        put(URL + "/{id}", qrCode.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"memberId": %d}
                                        """.formatted(deletedMember.getId()))
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("Участник с id " + deletedMember.getId() + " не найден"));

        QrCode stored = qrCodeRepository
                .findById(qrCode.getId())
                .orElseThrow();

        assertEquals(originalMember.getId(), stored.getMember().getId());
        assertEquals(qrCode.getCode(), stored.getCode());
    }

    // Проверяем, что обновление без memberId возвращает 400 и не меняет QR-код.
    @Test
    void update_whenMemberIdMissing_returnsBadRequestAndKeepsQrCode() throws Exception {
        Member member = saveMember();
        QrCode qrCode = saveQrCode(member);

        mockMvc.perform(
                        put(URL + "/{id}", qrCode.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value(startsWith("memberId: ")));

        QrCode stored = qrCodeRepository
                .findById(qrCode.getId())
                .orElseThrow();

        assertEquals(member.getId(), stored.getMember().getId());
        assertEquals(qrCode.getCode(), stored.getCode());
    }

    // Проверяем удаление QR-кода с сохранением его владельца в базе.
    @Test
    void delete_whenQrCodeExists_removesQrCodeAndKeepsMember() throws Exception {
        Member member = saveMember();
        QrCode qrCode = saveQrCode(member);

        mockMvc.perform(delete(URL + "/{id}", qrCode.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        assertTrue(qrCodeRepository.findById(qrCode.getId()).isEmpty());
        assertTrue(memberRepository.findByIdAndDeletedAtIsNull(member.getId()).isPresent());
    }

    // Проверяем, что удаление несуществующего QR-кода возвращает 404 и сохраняет остальные записи.
    @Test
    void delete_whenQrCodeMissing_returnsNotFoundAndKeepsExistingQrCode() throws Exception {
        Member member = saveMember();
        QrCode qrCode = saveQrCode(member);

        mockMvc.perform(delete(URL + "/{id}", MISSING_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message")
                        .value("QR-код с id " + MISSING_ID + " не найден"));

        QrCode stored = qrCodeRepository
                .findById(qrCode.getId())
                .orElseThrow();

        assertEquals(qrCode.getCode(), stored.getCode());
        assertEquals(member.getId(), stored.getMember().getId());
    }

    // Проверяем получение нужной страницы QR-кодов с сортировкой по ID.
    @Test
    void getQrCodes_withoutFilter_returnsRequestedPage() throws Exception {
        Member member = saveMember();

        saveQrCode(member);
        QrCode second = saveQrCode(member);
        saveQrCode(member);

        mockMvc.perform(
                        get(URL)
                                .param("page", "1")
                                .param("size", "1")
                                .param("sort", "id,asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(second.getId()))
                .andExpect(jsonPath("$.content[0].code")
                        .value(second.getCode().toString()))
                .andExpect(jsonPath("$.content[0].memberId")
                        .value(member.getId()))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    // Проверяем, что фильтр memberId возвращает только QR-коды выбранного участника.
    @Test
    void getQrCodes_withMemberId_returnsOnlyTheirQrCodes() throws Exception {
        Member selectedMember = saveMember();
        Member otherMember = saveMember();

        QrCode first = saveQrCode(selectedMember);
        saveQrCode(otherMember);
        QrCode second = saveQrCode(selectedMember);

        mockMvc.perform(
                        get(URL)
                                .param("memberId", selectedMember.getId().toString())
                                .param("page", "0")
                                .param("size", "10")
                                .param("sort", "id,asc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(first.getId()))
                .andExpect(jsonPath("$.content[1].id").value(second.getId()))
                .andExpect(jsonPath("$.content[0].memberId")
                        .value(selectedMember.getId()))
                .andExpect(jsonPath("$.content[1].memberId")
                        .value(selectedMember.getId()))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    private @NonNull Member saveMember() {
        Member member = new Member();
        member.setFirstName("Иван");
        member.setMiddleName("Петрович");
        member.setSurname("Петров");

        return memberRepository.saveAndFlush(member);
    }

    private @NonNull QrCode saveQrCode(Member member) {
        QrCode qrCode = new QrCode();
        qrCode.setCode(UUID.randomUUID());
        qrCode.setMember(member);

        return qrCodeRepository.saveAndFlush(qrCode);
    }
}