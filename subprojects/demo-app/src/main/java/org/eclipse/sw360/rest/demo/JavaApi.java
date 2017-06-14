/*
 * Copyright Siemens AG, 2017. Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.sw360.datahandler.thrift.MainlineState;
import org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.ReleaseRelationship;
import org.eclipse.sw360.datahandler.thrift.components.ClearingState;
import org.eclipse.sw360.datahandler.thrift.components.ComponentType;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectType;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.util.*;

public class JavaApi {

    private final String REST_SERVER_URL;
    private final String AUTH_SERVER_URL;

    private RestTemplate restTemplate = new RestTemplate();
    private ObjectMapper objectMapper = new ObjectMapper();
    private String accessToken;
    private String projectsUrl;
    private String componentsUrl;
    private String releasesUrl;
    private String vendorsUrl;
    private String licensesUrl;

    public JavaApi(String restServerURL, String authServerUrl) {
        REST_SERVER_URL = restServerURL;
        AUTH_SERVER_URL = authServerUrl;
    }

    public URI createProject(String name,
                             String description,
                             String version,
                             List<URI> releaseURIs,
                             String businessUnit,
                             Map<String, String> externalIds) throws Exception {
        Map<String,ProjectReleaseRelationship> releaseIdToUsage = new HashMap<>();
        for(URI uri: releaseURIs) {
            releaseIdToUsage.put(uri.getPath(),
                    new ProjectReleaseRelationship(ReleaseRelationship.CONTAINED, MainlineState.MAINLINE));
        }

        Map<String, Object> project = new HashMap<>();
        project.put("name", name);
        project.put("version", version);
        project.put("description", description);
        project.put("projectType", ProjectType.PRODUCT.toString());
        project.put("businessUnit", businessUnit);
        project.put("releaseIdToUsage", releaseIdToUsage);
        project.put("externalIds", externalIds);

        HttpEntity<String> httpEntity = getHttpEntity(project);

        URI location = restTemplate.postForLocation(projectsUrl, httpEntity);
        return location;
    }

    public URI createComponent(String name, URI vendorUri) throws Exception {
        Map<String, Object> component = new HashMap<>();
        component.put("name", name);
        component.put("description", name + " is part of the Spring framework");
        component.put("componentType", ComponentType.OSS.toString());
        component.put("vendors", Collections.singletonList(vendorUri));

        HttpEntity<String> httpEntity = getHttpEntity(component);

        URI location = restTemplate.postForLocation(componentsUrl, httpEntity);
        return location;
    }

    public URI createRelease(String name,
                             String version,
                             URI componentURI,
                             URI vendorUri,
                             List<URI> licenseUris,
                             Map<String, String> externalIds) throws Exception {
        Map<String, Object> release = new HashMap<>();
        release.put("name", name);
        release.put("componentId", componentURI.toString());
        release.put("vendorId", vendorUri.toString());
        release.put("version", version);
        release.put("clearingState", ClearingState.APPROVED.toString());
        release.put("externalIds", externalIds);
        release.put("mainLicenseIds", licenseUris);

        HttpEntity<String> httpEntity = getHttpEntity(release);

        URI location = restTemplate.postForLocation(releasesUrl, httpEntity);
        return location;
    }

    public URI createVendor(String fullName, String shortName, URL url) throws Exception {
        Map<String, Object> vendor = new HashMap<>();
        vendor.put("fullName", fullName);
        vendor.put("shortName", shortName);
        vendor.put("url", url);

        HttpEntity<String> httpEntity = getHttpEntity(vendor);

        URI location = restTemplate.postForLocation(vendorsUrl, httpEntity);
        return location;
    }

    public URI createLicense(String fullName, String shortName, String text) throws Exception {
        Map<String, Object> license = new HashMap<>();
        license.put("fullName", fullName);
        license.put("shortName", shortName);
        license.put("text", text);

        HttpEntity<String> httpEntity = getHttpEntity(license);

        URI location = restTemplate.postForLocation(licensesUrl, httpEntity);
        return location;
    }

    public void getLinksFromApiRoot() throws Exception {
        Map<String, Object> dummy = new HashMap<>();
        HttpEntity<String> httpEntity = getHttpEntity(dummy);

        ResponseEntity<String> response =
                restTemplate.exchange(REST_SERVER_URL + "/api",
                        HttpMethod.GET,
                        httpEntity,
                        String.class);

        JsonNode responseNode = new ObjectMapper().readTree(response.getBody());

        JsonNode linksNode = responseNode.get("_links");
        String curieName = linksNode.get("curies").get(0).get("name").asText();
        this.projectsUrl = linksNode.get(curieName + ":projects").get("href").asText();
        this.componentsUrl = linksNode.get(curieName + ":components").get("href").asText();
        this.releasesUrl = linksNode.get(curieName + ":releases").get("href").asText();
        this.vendorsUrl = linksNode.get(curieName + ":vendors").get("href").asText();
        this.licensesUrl = linksNode.get(curieName + ":licenses").get("href").asText();
    }

    private HttpEntity<String> getHttpEntity(Map<String, Object> component) throws IOException {
        String jsonBody = this.objectMapper.writeValueAsString(component);
        HttpHeaders headers = getHeadersWithBearerToken(getAccessToken());
        return new HttpEntity<>(jsonBody, headers);
    }

    private String getAccessToken() throws IOException {
        if (this.accessToken == null) {
            HttpHeaders headers = getHeadersForAccessToken();
            HttpEntity<String> httpEntity = new HttpEntity<>(headers);

            ResponseEntity<String> response =
                    restTemplate
                            .postForEntity(AUTH_SERVER_URL + "/oauth/token?"
                                            + "grant_type=password&username=admin@sw360.org&password=sw360-admin-password",
                                    httpEntity,
                                    String.class);

            String responseText = response.getBody();
            HashMap jwtMap = new ObjectMapper().readValue(responseText, HashMap.class);
            this.accessToken = (String) jwtMap.get("access_token");
        }
        return this.accessToken;
    }

    private HttpHeaders getHeadersForAccessToken() throws UnsupportedEncodingException {
        String clientCredentials = "trusted-sw360-client:sw360-secret";
        String base64ClientCredentials =
                Base64.getEncoder().encodeToString(clientCredentials.getBytes("utf-8"));

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + base64ClientCredentials);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        return headers;
    }

    private HttpHeaders getHeadersWithBearerToken(String accessToken) throws UnsupportedEncodingException {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        return headers;
    }
}
