/*
 * Copyright 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.eclipse.appengine.localserver.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.localserver.server.LocalAppEngineServerBehaviour.PortProber;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LocalAppEngineServerBehaviourTest {

  private LocalAppEngineServerBehaviour serverBehavior = new LocalAppEngineServerBehaviour();
  @Mock private PortProber portProber;

  @Test
  public void testAdminPort_initializedTo8000() {
    assertEquals(8000, serverBehavior.adminPort);
  }

  @Test
  public void testCheckAndSetPorts() throws CoreException {
    serverBehavior.serverPort = 65535;
    serverBehavior.checkAndSetPorts(portProber);

    assertEquals(65535, serverBehavior.serverPort);
    assertEquals(8000, serverBehavior.adminPort);
  }

  @Test
  public void testCheckAndSetPorts_negativeServerPort() {
    try {
      serverBehavior.serverPort = -1;
      serverBehavior.checkAndSetPorts(portProber);
      fail();
    } catch (CoreException ex) {
      assertEquals("Port must be between 0 and 65535.", ex.getMessage());
    }
  }

  @Test
  public void testCheckAndSetPorts_outOfBoundServerPort() {
    try {
      serverBehavior.serverPort = 65536;
      serverBehavior.checkAndSetPorts(portProber);
      fail();
    } catch (CoreException ex) {
      assertEquals("Port must be between 0 and 65535.", ex.getMessage());
    }
  }

  @Test
  public void testCheckAndSetPorts_serverPortInUse() {
    try {
      when(portProber.isPortInUse(8080)).thenReturn(true);

      serverBehavior.serverPort = 8080;
      serverBehavior.checkAndSetPorts(portProber);
      fail();
    } catch (CoreException ex) {
      assertEquals("Port 8080 is in use.", ex.getMessage());
    }
  }

  @Test
  public void testCheckAndSetPorts_adminPortInUse() throws CoreException {
    when(portProber.isPortInUse(8000)).thenReturn(true);

    serverBehavior.serverPort = 65535;
    serverBehavior.checkAndSetPorts(portProber);
    assertEquals(65535, serverBehavior.serverPort);
    assertEquals(0, serverBehavior.adminPort);
  }

  @Test
  public void testExtractPortFromUrl_hostName() {
    int port = LocalAppEngineServerBehaviour.extractPortFromUrl("http://localhost:5678");
    assertEquals(5678, port);

    port = LocalAppEngineServerBehaviour.extractPortFromUrl("http://my-machine:1234");
    assertEquals(1234, port);

    port = LocalAppEngineServerBehaviour.extractPortFromUrl("http://www.server-example.com:80");
    assertEquals(80, port);
  }

  @Test
  public void testExtractPortFromUrl_ipv4Address() {
    int port = LocalAppEngineServerBehaviour.extractPortFromUrl("http://0.0.0.0:5678");
    assertEquals(5678, port);

    port = LocalAppEngineServerBehaviour.extractPortFromUrl("http://192.168.1.4:1234");
    assertEquals(1234, port);
  }

  @Test
  public void testExtractPortFromUrl_noPortUrl() {
    int port = LocalAppEngineServerBehaviour.extractPortFromUrl("http://localhost");
    assertEquals(-1, port);
  }

  @Test
  public void testExtractPortFromUrl_noMatch() {
    int port = LocalAppEngineServerBehaviour.extractPortFromUrl("arbitrary string");
    assertEquals(-1, port);
  }

  private static final String[] serverOutput = new String[] {
      "WARNING  2016-11-03 21:11:21,930 devappserver2.py:785] DEFAULT_VERSION_HOSTNAME will not be set correctly with --port=0",
      "INFO     2016-11-03 21:11:21,956 api_server.py:205] Starting API server at: http://localhost:52892",
      "INFO     2016-11-03 21:11:21,959 dispatcher.py:197] Starting module \"default\" running at: http://localhost:55948",
      "INFO     2016-11-03 21:11:21,959 admin_server.py:116] Starting admin server at: http://localhost:43679",
      "Nov 03, 2016 9:11:23 PM com.google.appengine.tools.development.SystemPropertiesManager setSystemProperties"
  };

  @Test
  public void testExtractServerPortFromOutput() {
    serverBehavior.serverPort = 0;
    for (String line : serverOutput) {
      serverBehavior.new DevAppServerOutputListener().onOutputLine(line);
    }
    assertEquals(55948, serverBehavior.serverPort);
  }

  @Test
  public void testExtractAdminPortFromOutput() {
    serverBehavior.adminPort = 0;
    for (String line : serverOutput) {
      serverBehavior.new DevAppServerOutputListener().onOutputLine(line);
    }
    assertEquals(43679, serverBehavior.adminPort);
  }
}
