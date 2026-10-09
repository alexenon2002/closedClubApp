package ru.shim.closedclub.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MemberTest {

    @Test
    void getFullName_withoutMiddleName() {
        Member member = new Member();
        member.setFirstName("Иван");
        member.setSurname("Петров");
        String actual = member.getFullName();

        assertEquals("Иван Петров", actual);
    }

    @Test
    void getFullName_withMiddleName() {
        Member member = new Member();
        member.setFirstName("Иван");
        member.setMiddleName("Петрович");
        member.setSurname("Петров");
        String actual = member.getFullName();

        assertEquals("Иван Петрович Петров", actual);
    }

    @Test
    void getFullName_withMiddleNameIsEmpty() {
        Member member = new Member();
        member.setFirstName("Иван");
        member.setMiddleName("");
        member.setSurname("Петров");
        String actual = member.getFullName();

        assertEquals("Иван Петров", actual);
    }

    @Test
    void getFullName_withBlankMiddleName() {
        Member member = new Member();
        member.setFirstName("Иван");
        member.setMiddleName("   ");
        member.setSurname("Петров");
        String actual = member.getFullName();

        assertEquals("Иван Петров", actual);

    }
}
