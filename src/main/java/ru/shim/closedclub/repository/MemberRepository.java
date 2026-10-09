package ru.shim.closedclub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.shim.closedclub.entity.Member;
import ru.shim.closedclub.exception.ResourceNotFoundException;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Page<Member> findAllByDeletedAtIsNull(Pageable pageable);

    Page<Member> findBySurnameContainingIgnoreCaseAndDeletedAtIsNull(
            String surname, Pageable pageable);

    Optional<Member> findByIdAndDeletedAtIsNull(Long id);

    default Member getByIdOrThrow(Long id) {
        return findByIdAndDeletedAtIsNull(id).orElseThrow(() -> new ResourceNotFoundException("Участник с id " + id + " не найден"));
    }
}
