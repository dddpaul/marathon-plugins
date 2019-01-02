package com.github.dddpaul.marathon.plugin.fileauth;

import com.github.dddpaul.marathon.plugin.auth.Action;
import com.github.dddpaul.marathon.plugin.auth.JavaIdentity;
import mesosphere.marathon.Protos;
import mesosphere.marathon.plugin.auth.AuthorizedAction;
import mesosphere.marathon.state.AppDefinition;
import org.apache.mesos.Protos.CommandInfo;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


class FileAuthorizerTest {

    private static String FILENAME = "authorizer.conf.json";
    private JsObject JSON;

    static Stream<Arguments> validUsers() {
        return Stream.of(
                Arguments.of("guest", "/", Action.ViewApp),
                Arguments.of("guest", "/", Action.ViewGroup),
                Arguments.of("guest", "/", Action.ViewResource),
                Arguments.of("ernie", "/", Action.CreateApp),
                Arguments.of("ernie", "/", Action.UpdateGroup),
                Arguments.of("ernie", "/", Action.DeleteResource)
        );
    }

    static Stream<Arguments> invalidUsers() {
        return Stream.of(
                Arguments.of("guest", "/", Action.CreateApp),
                Arguments.of("guest", "/", Action.UpdateGroup),
                Arguments.of("guest", "/", Action.DeleteResource)
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
    @SuppressWarnings("unchecked")
    void shouldAuthorizeForValidPermissions(String login, String path, Action action) {
        FileAuthorizer authorizer = new FileAuthorizer();
        authorizer.initialize(null, JSON);
        AppDefinition app = AppDefinition.fromProto(
                Protos.ServiceDefinition.newBuilder()
                        .setId(path)
                        .setCmd(CommandInfo.newBuilder().build())
                        .setInstances(1)
                        .setExecutor("")
                        .build()
        );
        assertTrue(authorizer.isAuthorized(new JavaIdentity(login), (AuthorizedAction) action.getAction(), app));
    }

    @ParameterizedTest
    @MethodSource("invalidUsers")
    @SuppressWarnings("unchecked")
    void shouldNotAuthorizeForInvalidPermissions(String login, String path, Action action) {
        FileAuthorizer authorizer = new FileAuthorizer();
        authorizer.initialize(null, JSON);
        AppDefinition app = AppDefinition.fromProto(
                Protos.ServiceDefinition.newBuilder()
                        .setId(path)
                        .setCmd(CommandInfo.newBuilder().build())
                        .setInstances(1)
                        .setExecutor("")
                        .build()
        );
        assertFalse(authorizer.isAuthorized(new JavaIdentity(login), (AuthorizedAction) action.getAction(), app));
    }

}
