package com.github.dddpaul.marathon.plugin.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dddpaul.marathon.plugin.auth.conf.AuthorizerConfiguration;
import com.github.dddpaul.marathon.plugin.auth.entities.Permission;
import com.github.dddpaul.marathon.plugin.auth.entities.Principal;
import com.github.dddpaul.marathon.plugin.auth.entities.Role;
import mesosphere.marathon.plugin.auth.AuthorizedAction;
import mesosphere.marathon.plugin.auth.Identity;
import mesosphere.marathon.plugin.http.HttpResponse;
import mesosphere.marathon.plugin.plugin.PluginConfiguration;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.api.libs.json.JsObject;

import java.io.IOException;
import java.util.List;

public class Authorizer implements mesosphere.marathon.plugin.auth.Authorizer, PluginConfiguration {

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private AuthorizerConfiguration configuration;

    @Override
    public void initialize(scala.collection.immutable.Map<String, Object> marathonInfo, JsObject json) {
        logger.info("Authorizer initialize has been called with: " + json);
        ObjectMapper mapper = new ObjectMapper();
        try {
            configuration = mapper.readValue(json.toString(), AuthorizerConfiguration.class);
        } catch (IOException e) {
            logger.error("Plugin authorizer configuration parsing has been failed", e);
            configuration = new AuthorizerConfiguration();
        }
    }

    @Override
    public <Resource> boolean isAuthorized(Identity identity, AuthorizedAction<Resource> action, Resource resource) {
        if (!(identity instanceof Principal)) {
            return false;
        }
        Principal principal = (Principal) identity;

        List<Permission> permissions = configuration.getPermissions().get(principal.getName());
        if (CollectionUtils.isEmpty(permissions)) {
            logger.warn("{} has no permissions at all", principal);
            return false;
        }

        for (Permission p : permissions) {
            Role role = configuration.getRoles().get(p.getRoleName());
            if (role == null) {
                logger.error("Role {} is not found", p.getRoleName());
                continue;
            }

            try {
                if (p.check(role, action, resource)) {
                    return true;
                }
            } catch (Exception e) {
                logger.error("{} permission check error for {}", action, principal, e);
            }
        }

        logger.warn("{} has no {} permission to {}", principal, action, resource);
        return false;
    }

    @Override
    public void handleNotAuthorized(Identity principal, HttpResponse response) {
        response.status(403);
        response.body("application/json", "{\"problem\": \"Not Authorized to perform this action!\"}".getBytes());
    }
}
