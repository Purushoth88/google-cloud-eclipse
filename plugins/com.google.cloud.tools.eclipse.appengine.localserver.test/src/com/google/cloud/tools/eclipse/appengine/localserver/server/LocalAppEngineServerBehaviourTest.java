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
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.appengine.localserver.server.LocalAppEngineServerBehaviour.PortProber;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.server.core.IServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class LocalAppEngineServerBehaviourTest {

  private LocalAppEngineServerBehaviour serverBehavior = new LocalAppEngineServerBehaviour();
  @Mock private PortProber portProber;
  @Mock private IServer server;

  @Test
  public void testAdminPort_initializedTo8000() {
    assertEquals(8000, serverBehavior.adminPort);
  }

  @Test
  public void testCheckAndSetPorts() throws CoreException {
    when(server.getAttribute(eq("appEngineDevServerPort"), anyInt())).thenReturn(65535);
    serverBehavior.checkAndSetPorts(server, portProber);

    assertEquals(65535, serverBehavior.serverPort);
    assertEquals(8000, serverBehavior.adminPort);
  }

  @Test
  public void testCheckAndSetPorts_negativeServerPort() {
    try {
      when(server.getAttribute(eq("appEngineDevServerPort"), anyInt())).thenReturn(-1);
      serverBehavior.checkAndSetPorts(server, portProber);
      fail();
    } catch (CoreException ex) {
      assertEquals("Port must be between 0 and 65535.", ex.getMessage());
    }
  }

  @Test
  public void testCheckAndSetPorts_outOfBoundServerPort() {
    try {
      when(server.getAttribute(eq("appEngineDevServerPort"), anyInt())).thenReturn(65536);
      serverBehavior.checkAndSetPorts(server, portProber);
      fail();
    } catch (CoreException ex) {
      assertEquals("Port must be between 0 and 65535.", ex.getMessage());
    }
  }

  @Test
  public void testCheckAndSetPorts_serverPortInUse() {
    try {
      when(portProber.isPortInUse(8080)).thenReturn(true);

      when(server.getAttribute(eq("appEngineDevServerPort"), anyInt())).thenReturn(8080);
      serverBehavior.checkAndSetPorts(server, portProber);
      fail();
    } catch (CoreException ex) {
      assertEquals("Port 8080 is in use.", ex.getMessage());
    }
  }

  @Test
  public void testCheckAndSetPorts_adminPortInUseFailover() throws CoreException {
    when(portProber.isPortInUse(8000)).thenReturn(true);

    when(server.getAttribute(eq("appEngineDevServerPort"), anyInt())).thenReturn(65535);
    when(server.getAttribute(eq("failoverAdminPortBound"), anyBoolean())).thenReturn(true);
    serverBehavior.checkAndSetPorts(server, portProber);
    assertEquals(65535, serverBehavior.serverPort);
    assertEquals(0, serverBehavior.adminPort);
  }

  @Test
  public void testCheckAndSetPorts_adminPortInUseNoFailover() {
    try {
      when(portProber.isPortInUse(8000)).thenReturn(true);

      when(server.getAttribute(eq("appEngineDevServerPort"), anyInt())).thenReturn(65535);
      when(server.getAttribute(eq("failoverAdminPortBound"), anyBoolean())).thenReturn(false);
      serverBehavior.checkAndSetPorts(server, portProber);
      assertEquals(65535, serverBehavior.serverPort);
      assertEquals(0, serverBehavior.adminPort);
    } catch (CoreException ex) {
      assertEquals("Default admin port 8000 is in use.", ex.getMessage());
    }
  }

  @Test
  public void testExtractPortFromServerUrlOutput_hostName() {
    int port = LocalAppEngineServerBehaviour.extractPortFromServerUrlOutput("http://localhost:567");
    assertEquals(567, port);

    port = LocalAppEngineServerBehaviour.extractPortFromServerUrlOutput("http://my-machine:1234");
    assertEquals(1234, port);

    port = LocalAppEngineServerBehaviour.extractPortFromServerUrlOutput("http://www.a-b.c.com:80");
    assertEquals(80, port);
  }

  @Test
  public void testExtractPortFromServerUrlOutput_ipv4Address() {
    int port = LocalAppEngineServerBehaviour.extractPortFromServerUrlOutput("http://0.0.0.0:5678");
    assertEquals(5678, port);

    port = LocalAppEngineServerBehaviour.extractPortFromServerUrlOutput("http://192.168.1.4:1234");
    assertEquals(1234, port);
  }

  @Test
  public void testExtractPortFromServerUrlOutput_noPortUrl() {
    int port = LocalAppEngineServerBehaviour.extractPortFromServerUrlOutput("http://localhost");
    assertEquals(-1, port);
  }

  @Test
  public void testExtractPortFromDevServerUrlOutput_noMatch() {
    int port = LocalAppEngineServerBehaviour.extractPortFromServerUrlOutput("arbitrary string");
    assertEquals(-1, port);
  }

  private static final String[] serverOutputWithDefaultModule1 = new String[] {
      "WARNING  2016-11-03 21:11:21,930 devappserver2.py:785] DEFAULT_VERSION_HOSTNAME will not be set correctly with --port=0",
      "INFO     2016-11-03 21:11:21,956 api_server.py:205] Starting API server at: http://localhost:52892",
      "INFO     2016-11-03 21:11:21,959 dispatcher.py:197] Starting module \"default\" running at: http://localhost:55948",
      "INFO     2016-11-03 21:11:21,959 dispatcher.py:197] Starting module \"second\" running at: http://localhost:8081",
      "INFO     2016-11-03 21:11:21,959 admin_server.py:116] Starting admin server at: http://localhost:43679",
      "Nov 03, 2016 9:11:23 PM com.google.appengine.tools.development.SystemPropertiesManager setSystemProperties"
  };

  private static final String[] serverOutputWithDefaultModule2 = new String[] {
      "WARNING  2016-11-03 21:11:21,930 devappserver2.py:785] DEFAULT_VERSION_HOSTNAME will not be set correctly with --port=0",
      "INFO     2016-11-03 21:11:21,956 api_server.py:205] Starting API server at: http://localhost:52892",
      "INFO     2016-11-03 21:11:21,959 dispatcher.py:197] Starting module \"first\" running at: http://localhost:55948",
      "INFO     2016-11-03 21:11:21,959 dispatcher.py:197] Starting module \"default\" running at: http://localhost:8081",
      "INFO     2016-11-03 21:11:21,959 admin_server.py:116] Starting admin server at: http://localhost:43679",
      "Nov 03, 2016 9:11:23 PM com.google.appengine.tools.development.SystemPropertiesManager setSystemProperties"
  };

  private static final String[] serverOutputWithNoDefaultModule = new String[] {
      "WARNING  2016-11-03 21:11:21,930 devappserver2.py:785] DEFAULT_VERSION_HOSTNAME will not be set correctly with --port=0",
      "INFO     2016-11-03 21:11:21,956 api_server.py:205] Starting API server at: http://localhost:52892",
      "INFO     2016-11-03 21:11:21,959 dispatcher.py:197] Starting module \"first\" running at: http://localhost:8181",
      "INFO     2016-11-03 21:11:21,959 dispatcher.py:197] Starting module \"second\" running at: http://localhost:8182",
      "INFO     2016-11-03 21:11:21,959 dispatcher.py:197] Starting module \"third\" running at: http://localhost:8183",
      "INFO     2016-11-03 21:11:21,959 admin_server.py:116] Starting admin server at: http://localhost:43679",
      "Nov 03, 2016 9:11:23 PM com.google.appengine.tools.development.SystemPropertiesManager setSystemProperties"
  };

  @Test
  public void testExtractServerPortFromOutput_firstModuleIsDefault() throws CoreException {
    when(server.getAttribute(eq("appEngineDevServerPort"), anyInt())).thenReturn(0);
    serverBehavior.checkAndSetPorts(server, portProber);

    simulateOutputParsing(serverOutputWithDefaultModule1);
    assertEquals(55948, serverBehavior.serverPort);
  }

  @Test
  public void testExtractServerPortFromOutput_secondModuleIsDefault() throws CoreException {
    when(server.getAttribute(eq("appEngineDevServerPort"), anyInt())).thenReturn(0);
    serverBehavior.checkAndSetPorts(server, portProber);

    simulateOutputParsing(serverOutputWithDefaultModule2);
    assertEquals(8081, serverBehavior.serverPort);
  }

  @Test
  public void testExtractServerPortFromOutput_noDefaultModule() throws CoreException {
    when(server.getAttribute(eq("appEngineDevServerPort"), anyInt())).thenReturn(0);
    serverBehavior.checkAndSetPorts(server, portProber);

    simulateOutputParsing(serverOutputWithNoDefaultModule);
    assertEquals(8181, serverBehavior.serverPort);
  }

  @Test
  public void testExtractServerPortFromOutput_defaultModuleDoesNotOverrideUserSpecifiedPort()
      throws CoreException {
    when(server.getAttribute(eq("appEngineDevServerPort"), anyInt())).thenReturn(12345);
    serverBehavior.checkAndSetPorts(server, portProber);

    simulateOutputParsing(serverOutputWithDefaultModule1);
    assertEquals(12345, serverBehavior.serverPort);
  }

  @Test
  public void testExtractAdminPortFromOutput() throws CoreException {
    when(portProber.isPortInUse(8000)).thenReturn(true);
    when(server.getAttribute(eq("failoverAdminPortBound"), anyBoolean())).thenReturn(true);
    serverBehavior.checkAndSetPorts(server, portProber);

    simulateOutputParsing(serverOutputWithDefaultModule1);
    assertEquals(43679, serverBehavior.adminPort);
  }

  private void simulateOutputParsing(String[] output) {
    LocalAppEngineServerBehaviour.DevAppServerOutputListener outputListener =
        serverBehavior.new DevAppServerOutputListener();
    for (String line : output) {
      outputListener.onOutputLine(line);
    }
  }
}
