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
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.thrift.components.Component;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.rest.resourceserver.user.Sw360UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Sw360ComponentService {
    @Value("${sw360.thrift-server-url}")
    private String thriftServerUrl;

    @NonNull
    private final Sw360UserService sw360UserService;

    public List<Component> getComponentsForUser(String userId) {
        try {
            User sw360User = sw360UserService.getUserById(userId);
            ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
            List<Component> components = sw360ComponentClient.getMyComponents(sw360User);
            return components;
        } catch (TException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public Component getComponentForUserById(String componentId, String userId) {
        try {
            ComponentService.Iface sw360ComponentClient = getThriftComponentClient();
            User sw360User = sw360UserService.getUserById(userId);
            sw360ComponentClient.getComponentById(componentId, sw360User);
        } catch (TException e) {
            e.printStackTrace();
        }
        return null;
    }

    private ComponentService.Iface getThriftComponentClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/components/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new ComponentService.Client(protocol);
    }
}