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

package com.google.cloud.tools.eclipse.util.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.google.cloud.tools.eclipse.util.io.SocketUtil.ServerSocketCreator;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SocketUtilTest {

  @Mock private ServerSocketCreator socketCreator;
  @Mock private ServerSocket serverSocket;

  @Test
  public void testFindFreePort_zeroPortCount() throws IOException {
    when(socketCreator.newServerSocket()).thenReturn(serverSocket);
    assertTrue(SocketUtil.findFreePorts(0, socketCreator).isEmpty());
  }

  @Test
  public void testFindFreePort_negativePortCount() throws IOException {
    when(socketCreator.newServerSocket()).thenReturn(serverSocket);
    assertTrue(SocketUtil.findFreePorts(-1, socketCreator).isEmpty());
  }

  @Test
  public void testFindFreePort() throws IOException {
    when(socketCreator.newServerSocket()).thenReturn(serverSocket);
    when(serverSocket.getLocalPort()).thenReturn(43210);

    List<Integer> ports = SocketUtil.findFreePorts(1, socketCreator);
    assertEquals(Arrays.asList(43210), ports);
  }

  @Test
  public void testFindFreePort_threePorts() throws IOException {
    when(socketCreator.newServerSocket()).thenReturn(serverSocket);
    when(serverSocket.getLocalPort()).thenReturn(43210).thenReturn(1024).thenReturn(8080);

    List<Integer> ports = SocketUtil.findFreePorts(3, socketCreator);
    assertEquals(Arrays.asList(43210, 1024, 8080), ports);
  }

  @Test
  public void testFindFreePort_outOfFreePorts() throws IOException {
    when(socketCreator.newServerSocket()).thenReturn(serverSocket).thenThrow(new IOException());

    assertTrue(SocketUtil.findFreePorts(10, socketCreator).isEmpty());
  }
}
