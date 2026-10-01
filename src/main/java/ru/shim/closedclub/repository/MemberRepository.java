package ru.shim.closedclub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.shim.closedclub.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Page<Member> findByFullNameContainingIgnoreCase(
            String fullName, Pageable pageable);
}
