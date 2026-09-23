package ru.shim.closedclub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.shim.closedclub.entity.Member;

public interface MemberRepository extends JpaRepository <Member,Long> {
}
