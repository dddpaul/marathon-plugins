package com.github.dddpaul.marathon.plugin.auth;

import com.github.dddpaul.marathon.plugin.auth.entities.Principal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import play.api.libs.json.JsObject;
import play.api.libs.json.Json;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


class AuthenticatorTest {

    private static String FILENAME = "authenticator.conf.json";
    private JsObject JSON;

    static Stream<Arguments> validUsers() {
        return Stream.of(
                Arguments.of("ernie", "ernie"),
                Arguments.of("corp-admin", "qwerty"),
                Arguments.of("guest", "12345"),
                Arguments.of("md5_user", "qwe"),
                Arguments.of("sha1_user", "qwe"),
                Arguments.of("bcrypt_user", "qwe")
        );
    }

    static Stream<Arguments> invalidUsers() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of(null, ""),
                Arguments.of("", null),
                Arguments.of("", ""),
                Arguments.of("", "invalid"),
                Arguments.of("invalid", ""),
                Arguments.of("invalid", "invalid"),
                Arguments.of("ernie", null),
                Arguments.of("ernie", ""),
                Arguments.of("ernie", "invalid"),
                Arguments.of("md5_user", null),
                Arguments.of("md5_user", ""),
                Arguments.of("md5_user", "invalid"),
                Arguments.of("sha1_user", null),
                Arguments.of("sha1_user", ""),
                Arguments.of("sha1_user", "invalid"),
                Arguments.of("bcrypt_user", null),
                Arguments.of("bcrypt_user", ""),
                Arguments.of("bcrypt_user", "invalid")
        );
    }

    @BeforeEach
    void setUp() throws Exception {
        URL url = Optional.ofNullable(getClass().getClassLoader().getResource(FILENAME))
                .orElseThrow(() -> new RuntimeException(FILENAME + " is not found"));
        JSON = (JsObject) Json.parse(Files.readAllBytes(Paths.get(url.toURI())));
    }

    @ParameterizedTest
    @MethodSource("validUsers")
    void shouldAuthenticateForValidCredentials(String login, String password) {
        // given
        Authenticator authenticator = new Authenticator();

        // when
        authenticator.initialize(null, JSON);

        // then
        assertEquals(new Principal(login), authenticator.doAuth(login, password));
    }

    @ParameterizedTest
    @MethodSource("invalidUsers")
    void shouldNotAuthenticateForInvalidCredentials(String login, String password) {
        // given
        Authenticator authenticator = new Authenticator();

        // when
        authenticator.initialize(null, JSON);

        // then
        assertNull(authenticator.doAuth(login, password));
    }
}
