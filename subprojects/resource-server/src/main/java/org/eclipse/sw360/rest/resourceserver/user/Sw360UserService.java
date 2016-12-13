/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.sw360.rest.resourceserver.user;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.THttpClient;
import org.apache.thrift.transport.TTransportException;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class Sw360UserService {
    @Value("${sw360.thrift-server-url}")
    private String thriftServerUrl;

    public List<User> getAllUsers() {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            List<User> users = sw360UserClient.getAllUsers();
            return users;
        } catch (TException e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    public User getUserById(String id) {
        try {
            UserService.Iface sw360UserClient = getThriftUserClient();
            User user = sw360UserClient.getByEmail(id);
            return user;
        } catch (TException e) {
            e.printStackTrace();
        }
        return null;
    }

    private UserService.Iface getThriftUserClient() throws TTransportException {
        THttpClient thriftClient = new THttpClient(thriftServerUrl + "/users/thrift");
        TProtocol protocol = new TCompactProtocol(thriftClient);
        return new UserService.Client(protocol);
    }
}