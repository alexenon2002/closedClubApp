package ru.shim.closedclub.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.shim.closedclub.dto.member.MemberRequest;
import ru.shim.closedclub.dto.member.MemberResponse;
import ru.shim.closedclub.service.MemberService;


@RestController
@RequestMapping("/api/v1/members")
public class MemberController {
    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    public MemberResponse create(@Valid @RequestBody MemberRequest memberRequest) {
        return memberService.create(memberRequest);
    }

    @GetMapping
    public Page<MemberResponse> getMembers(@RequestParam(required = false) String surname, Pageable pageable) {
        return memberService.getMembers(surname, pageable);
    }

    @GetMapping("/{id}")
    public MemberResponse getById(@PathVariable long id) {
        return memberService.getById(id);
    }

    @PutMapping("/{id}")
    public MemberResponse update(@PathVariable long id,@Valid @RequestBody MemberRequest memberRequest) {
        return memberService.update(id, memberRequest);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        memberService.delete(id);
    }
}
