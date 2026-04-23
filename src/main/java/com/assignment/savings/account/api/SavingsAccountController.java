package com.assignment.savings.account.api;

import com.assignment.savings.account.service.SavingsAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
public class SavingsAccountController {

    private final SavingsAccountService savingsAccountService;

    public SavingsAccountController(SavingsAccountService savingsAccountService) {
        this.savingsAccountService = savingsAccountService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateSavingsAccountResponse createAccount(
            @Valid @RequestBody CreateSavingsAccountRequest request,
            HttpServletRequest httpServletRequest
    ) {
        httpServletRequest.setAttribute("transactionReference", request.transactionReference());
        return savingsAccountService.createAccount(request);
    }


    @GetMapping("/{id}")
    public SavingsAccountResponse getAccountById(@PathVariable Long id) {
        return savingsAccountService.getAccountById(id);
    }
}
