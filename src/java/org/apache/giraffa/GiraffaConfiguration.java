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
package org.apache.giraffa;

import org.apache.hadoop.conf.Configuration;

public class GiraffaConfiguration extends Configuration {
  public static final String  GRFA_URI_SCHEME = "grfa";
  public static final String  GRFA_JAR_FILE_KEY = "grfa.jar.file";
  public static final String  GRFA_JAR_FILE_DEFAULT = "dist/lib/grfa.jar";
  public static final String  GRFA_TABLE_NAME_KEY = "grfa.table.name";
  public static final String  GRFA_TABLE_NAME_DEFAULT = "Namespace";
  public static final String  GRFA_ROW_KEY_KEY = "grfa.rowkey.class";
  public static final String  GRFA_ROW_KEY_DEFAULT =
                              "org.apache.giraffa.FullPathRowKey";
  public static final String  GRFA_CACHING_KEY = "grfa.rowkey.caching";
  public static final Boolean GRFA_CACHING_DEFAULT = true;
  public static final String  GRFA_COPROCESSOR_KEY = "grfa.coprocessor.class"; 
  public static final String  GRFA_COPROCESSOR_DEFAULT =
                              "org.apache.hadoop.hdfs.BlockManagementAgent";
  public static final String  GRFA_HDFS_ADDRESS_KEY = "grfa.hdfs.address";
  public static final String  GRFA_HDFS_ADDRESS_DEFAULT = "file:///";
  public static final String  GRFA_HBASE_ADDRESS_KEY = "grfa.hbase.address";
  public static final String  GRFA_HBASE_ADDRESS_DEFAULT = "file:///";

  static {
    // adds the default resources
    Configuration.addDefaultResource("giraffa-default.xml");
    Configuration.addDefaultResource("giraffa-site.xml");
  }

  public GiraffaConfiguration() {
    super();
  }

  public GiraffaConfiguration(Configuration conf) {
    super(conf);
  }
}