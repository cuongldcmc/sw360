/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Component.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.component;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.rest.resourceserver.core.HalResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.hateoas.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@BasePathAwareController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ComponentController implements ResourceProcessor<RepositoryLinksResource> {
    private final String COMPONENTS_URL = "/components";
    
    @Value("${sw360.thrift-server-url}")
    private String thriftServerUrl;
    
    protected final EntityLinks entityLinks;

    @NonNull
    private Sw360ComponentService componentService;

    // @PreAuthorize("hasRole('ROLE_SW360_USER')")
    @RequestMapping(value = COMPONENTS_URL)
    public ResponseEntity<Resources<Resource>> getComponents(OAuth2Authentication oAuth2Authentication) {
        try {
            String userId = (String) oAuth2Authentication.getPrincipal();
            List<Component> components = componentService.getComponentsForUser(userId);

            List<Resource> componentResources = new ArrayList<>();
            for (Component component : components) {
                HalResource componentResource = createHalComponentResource(component);
                componentResources.add(componentResource);
            }
            Resources<Resource> resources = new Resources<>(componentResources);

            return new ResponseEntity<>(resources, HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @RequestMapping(COMPONENTS_URL + "/{id}")
    @ResponseBody
    public ResponseEntity<Resource> getComponent(
            @PathVariable("id") String id, OAuth2Authentication oAuth2Authentication) {
        try {
            String userId = (String) oAuth2Authentication.getPrincipal();
            Component sw360Component = componentService.getComponentForUserById(id, userId);
            HalResource userHalResource = createHalComponentResource(sw360Component);
            return new ResponseEntity<>(userHalResource, HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        resource.add(linkTo(ComponentController.class).slash("api/components").withRel("components"));
        return resource;
    }

    private HalResource createHalComponentResource(Component sw360Component) {
        ComponentResource componentResource = new ComponentResource();

        componentResource.setType(sw360Component.getType());
        componentResource.setComponentType(String.valueOf(sw360Component.getComponentType()));
        componentResource.setName(sw360Component.getName());
        componentResource.setDescription(sw360Component.getDescription());
        componentResource.setCreatedBy(sw360Component.getCreatedBy());
        componentResource.setCreatedOn(sw360Component.getCreatedOn());

        String componentUUID = sw360Component.getId();
        Link selfLink = linkTo(ComponentController.class).slash("api/components/" + componentUUID).withSelfRel();
        componentResource.add(selfLink);
        return new HalResource(componentResource);
    }
}