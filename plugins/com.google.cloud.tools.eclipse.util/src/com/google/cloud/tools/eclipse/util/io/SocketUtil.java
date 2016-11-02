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

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SocketUtil {

  /**
   * @param portCount the number of free ports to find
   * @return a list of free ports whose length is {@code portCount} if it was able to find
   *     {@code portCount} free ports; an empty list if it could not fulfill the request or
   *     {@code portCount} was negative or 0
   */
  public static List<Integer> findFreePorts(int portCount) {
    return findFreePorts(portCount, new ServerSocketCreator() {
      @Override
      public ServerSocket newServerSocket() throws IOException {
        return new ServerSocket(0);
      }
    });
  }

  @VisibleForTesting
  public interface ServerSocketCreator {
    ServerSocket newServerSocket() throws IOException;
  }

  @VisibleForTesting
  public static List<Integer> findFreePorts(int portCount, ServerSocketCreator socketCreator) {
    List<ServerSocket> serverSockets = new ArrayList<>();
    List<Integer> ports = new ArrayList<>();

    try {
      for (int i = 0; i < portCount; i++) {
        ServerSocket socket = socketCreator.newServerSocket();
        serverSockets.add(socket);
        ports.add(socket.getLocalPort());
      }
      return ports;

    } catch (IOException ex) {
      return Collections.emptyList();
    }
    finally {
      for (ServerSocket socket : serverSockets) {
        try {
          socket.close();
        } catch (IOException ex) {}
      }
    }
  }
}
