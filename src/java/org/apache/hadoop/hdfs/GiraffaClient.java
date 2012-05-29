/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hdfs;

import java.io.IOException;

import org.apache.giraffa.GiraffaConfiguration;
import org.apache.giraffa.NamespaceAgent;
import org.apache.hadoop.fs.FileSystem.Statistics;

/**
 * GiraffaClient is an extension of DFSClient
 * with the namenode proxy member replaced with NamespaceAgent.
 * This allows to reuse all the logic of DFSClient communicating
 * to NameNode and DataNodes for Giraffa.
 * NamespaceAgent seamlessly replaces client-to-NameNode communication 
 * with client-to-HBase communication, exchanging meta-data information with 
 * HBase instead of NameNode.
 * The client-to-DataNode communication remains unchanged.
 */
public class GiraffaClient extends DFSClient {

  public GiraffaClient(GiraffaConfiguration conf, Statistics stats)
  throws IOException, ClassNotFoundException {
    super(null, new NamespaceAgent(conf), conf, stats);
  }

  NamespaceAgent getNamespaceAgent() {
    return (NamespaceAgent) this.namenode;
  }

  public static void format(GiraffaConfiguration conf) throws IOException {
    NamespaceAgent.format(conf);
  }

  @Override // DFSClient
  public void close() throws IOException {
    super.close();
    getNamespaceAgent().close();
  }
}