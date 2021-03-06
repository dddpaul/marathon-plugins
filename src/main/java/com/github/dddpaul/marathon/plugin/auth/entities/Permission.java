package com.github.dddpaul.marathon.plugin.auth.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import mesosphere.marathon.plugin.auth.AuthorizedAction;
import mesosphere.marathon.plugin.auth.AuthorizedResource;
import mesosphere.marathon.state.AppDefinition;
import mesosphere.marathon.state.Group;
import mesosphere.marathon.state.PathId;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Getter
public class Permission {

    @JsonProperty("role")
    private String roleName;

    private String path;

    private Pattern pattern;

    public Permission setPath(String path) {
        this.path = path;
        if (pattern == null) {
            try {
                pattern = Pattern.compile(path);
            } catch (PatternSyntaxException ignored) {
            }
        }
        return this;
    }

    /**
     * Returns <code>true</code> if <code>role</code> has specified by <code>action</code> access to <code>resource</code>
     */
    public <Resource> boolean check(Role role, AuthorizedAction<?> action, Resource resource) {
        if (role.contains(action)) {
            PathId pathId;
            if (resource instanceof AppDefinition) {
                pathId = ((AppDefinition) resource).id();
            } else if (resource instanceof Group) {
                pathId = ((Group) resource).id();
            } else if (resource instanceof AuthorizedResource) {
                // Allow access to Marathon internal resources (events, metrics etc.)
                return true;
            } else {
                throw new RuntimeException("Unsupported resource: " + resource);
            }
            if (pathId.toString().startsWith(path)) {
                return true;
            }
            if (pattern != null) {
                return pattern.matcher(pathId.toString()).matches();
            }
        }
        return false;
    }
}
