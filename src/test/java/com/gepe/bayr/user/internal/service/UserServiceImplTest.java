package com.gepe.bayr.user.internal.service;

import com.gepe.bayr.user.internal.entity.User;
import com.gepe.bayr.user.internal.repo.RoleRepo;
import com.gepe.bayr.user.internal.repo.UserRepo;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    UserRepo userRepo;

    @Mock
    RoleRepo roleRepo;

    @InjectMocks
    UserServiceImpl service;

    @Nested
    class ExistsByEmail{
        record ExistsEmailCase(
                String name,
                String email,
                boolean repoResult
        ) {}

        static Stream<ExistsEmailCase> cases() {

            return Stream.of(
                    new ExistsEmailCase(
                            "email exists",
                            "john@test.com",
                            true
                    ),
                    new ExistsEmailCase(
                            "email not exists",
                            "new@test.com",
                            false
                    )
            );
        }

        @ParameterizedTest
        @MethodSource("cases")
        void shouldCheckEmail(ExistsEmailCase tc) {
            when(userRepo.existsByEmail(tc.email))
                    .thenReturn(tc.repoResult);
            boolean result = service.existsByEmail(tc.email);

            assertEquals(tc.repoResult,result);
        }
    }
    @Nested
    class ExistsByNickname{
        record ExistsNicknameCase(
                String name,
                String nickname,
                boolean repoResult
        ) {}

        static Stream<ExistsNicknameCase> cases() {

            return Stream.of(
                    new ExistsNicknameCase(
                            "nickname exists",
                            "foobar",
                            true
                    ),
                    new ExistsNicknameCase(
                            "nickname not exists",
                            "unknown",
                            false
                    )
            );
        }

        @ParameterizedTest
        @MethodSource("cases")
        void shouldCheckNickname(ExistsNicknameCase tc) {
            when(userRepo.existsByNickname(tc.nickname))
                    .thenReturn(tc.repoResult);
            boolean result = service.existsByNickname(tc.nickname);

            assertEquals(tc.repoResult,result);
        }
    }
}