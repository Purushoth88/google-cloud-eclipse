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
import java.util.ArrayList;
import java.util.Arrays;
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
      when(portProber.findFreePorts(1)).thenReturn(Arrays.asList(8180));

      serverBehavior.serverPort = 8080;
      serverBehavior.checkAndSetPorts(portProber);
      fail();
    } catch (CoreException ex) {
      assertEquals("Port 8080 is in use.", ex.getMessage());
    }
  }

  @Test
  public void testCheckAndSetPorts_serverPortZero() throws CoreException {
    when(portProber.findFreePorts(1)).thenReturn(Arrays.asList(8280));

    serverBehavior.serverPort = 0;
    serverBehavior.checkAndSetPorts(portProber);
    assertEquals(8280, serverBehavior.serverPort);
  }

  @Test
  public void testCheckAndSetPorts_serverPortZeroAndNoFreePort() {
    try {
      when(portProber.findFreePorts(1)).thenReturn(new ArrayList<Integer>());

      serverBehavior.serverPort = 0;
      serverBehavior.checkAndSetPorts(portProber);
    } catch (CoreException ex) {
      assertEquals("Failed to find a free port.", ex.getMessage());
    }
  }

  @Test
  public void testCheckAndSetPorts_adminPortInUse() throws CoreException {
    when(portProber.isPortInUse(8000)).thenReturn(true);
    when(portProber.findFreePorts(1)).thenReturn(Arrays.asList(8200));

    serverBehavior.serverPort = 65535;
    serverBehavior.checkAndSetPorts(portProber);
    assertEquals(65535, serverBehavior.serverPort);
    assertEquals(8200, serverBehavior.adminPort);
  }

  @Test
  public void testCheckAndSetPorts_adminPortInUseAndNoFreePort() {
    try {
      when(portProber.isPortInUse(8000)).thenReturn(true);
      when(portProber.findFreePorts(1)).thenReturn(new ArrayList<Integer>());

      serverBehavior.serverPort = 65535;
      serverBehavior.checkAndSetPorts(portProber);
      fail();
    } catch (CoreException ex) {
      assertEquals("Failed to find a free port.", ex.getMessage());
    }
  }

  @Test
  public void testCheckAndSetPorts_serverPortZeroAndAdminPortInUse() throws CoreException {
    when(portProber.isPortInUse(8000)).thenReturn(true);
    when(portProber.findFreePorts(2)).thenReturn(Arrays.asList(10000, 20000));

    serverBehavior.serverPort = 0;
    serverBehavior.checkAndSetPorts(portProber);
    assertEquals(10000, serverBehavior.serverPort);
    assertEquals(20000, serverBehavior.adminPort);
  }

}
