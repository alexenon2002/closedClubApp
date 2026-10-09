package ru.shim.closedclub.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "qr_codes")
public class QrCode extends BaseEntity {

    @Column(nullable = false, unique = true)
    private UUID code;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public UUID getCode() {
        return code;
    }

    public void setCode(UUID code) {
        this.code = code;
    }

}
