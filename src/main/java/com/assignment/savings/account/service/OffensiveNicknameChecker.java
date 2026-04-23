package com.assignment.savings.account.service;

import com.assignment.savings.account.domain.OffensiveNickname;
import com.assignment.savings.account.domain.OffensiveNicknameRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OffensiveNicknameChecker {

    private final OffensiveNicknameRepository offensiveNicknameRepository;

    public OffensiveNicknameChecker(OffensiveNicknameRepository offensiveNicknameRepository) {
        this.offensiveNicknameRepository = offensiveNicknameRepository;
    }

    // Isolates offensive nickname detection so account creation flow orchestration
    // does not need to know how nickname rules are stored or evaluated
    public boolean containsOffensiveNickname(String accountNickName) {
        if (!StringUtils.hasText(accountNickName)) {
            return false;
        }

        String normalized = accountNickName.toLowerCase(Locale.ROOT);
        List<OffensiveNickname> offensiveNicknames = offensiveNicknameRepository.findAll();

        return offensiveNicknames.stream()
                .map(OffensiveNickname::getValue)
                .filter(StringUtils::hasText)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(normalized::contains);
    }
}
