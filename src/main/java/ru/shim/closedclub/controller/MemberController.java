package ru.shim.closedclub.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.shim.closedclub.dto.member.MemberRequest;
import ru.shim.closedclub.dto.member.MemberResponse;
import ru.shim.closedclub.service.MemberService;


@RestController
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    public MemberResponse create(@RequestBody MemberRequest memberRequest) {
        return memberService.create(memberRequest);
    }

    @GetMapping
    public Page<MemberResponse> getMembers(@RequestParam(required = false) String fullName, Pageable pageable) {
        return memberService.getMembers(fullName, pageable);
    }

    @GetMapping("/{id}")
    public MemberResponse getById(@PathVariable long id) {
        return memberService.getById(id);
    }

    @PutMapping("/{id}")
    public MemberResponse update(@PathVariable long id, @RequestBody MemberRequest memberRequest) {
        return memberService.update(id, memberRequest);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        memberService.delete(id);
    }
}
