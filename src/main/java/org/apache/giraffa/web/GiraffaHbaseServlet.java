package org.apache.giraffa.web;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.giraffa.FileField;
import org.apache.giraffa.GiraffaConfiguration;
import org.apache.giraffa.RowKey;
import org.apache.giraffa.RowKeyFactory;
import org.apache.giraffa.hbase.NamespaceProcessor;
import org.apache.giraffa.web.GiraffaWebJsonWrappers.LocatedBlockDescriptor;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.coprocessor.MasterCoprocessorEnvironment;
import org.apache.hadoop.hbase.filter.CompareFilter;
import org.apache.hadoop.hbase.filter.RowFilter;
import org.apache.hadoop.hbase.filter.SubstringComparator;
import org.apache.hadoop.hdfs.protocol.LocatedBlock;
import org.codehaus.jackson.map.ObjectMapper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.giraffa.web.GiraffaWebJsonWrappers.DataTablesRequest;
import org.apache.giraffa.web.GiraffaWebJsonWrappers.DataTablesReply;

public class GiraffaHbaseServlet extends HttpServlet {

  private static final Log LOG = LogFactory.getLog(GiraffaFileServlet.class);
  private static final long serialVersionUID = 1L;

  private HTableInterface table;
  private ObjectMapper mapper = new ObjectMapper();

  private static final long BLOCK_RESULT_LIMIT = 20;
  private static final long PAGING_FORWARD_PRESCAN = 10;


  @Override
  public void init() throws ServletException {
    super.init();
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse response)
      throws ServletException, IOException {
    openTable();

    DataTablesRequest dataRequest = new DataTablesRequest(req);
    DataTablesReply dataReply = new DataTablesReply(dataRequest.getSEcho());

    Scan s = new Scan();
    s.setMaxVersions(1);
    s.setCaching(300);

    if (!StringUtils.isEmpty(dataRequest.getSSearch())) {
      SubstringComparator comp =
          new SubstringComparator(dataRequest.getSSearch());
      RowFilter filter = new RowFilter(CompareFilter.CompareOp.EQUAL, comp);
      s.setFilter(filter);
    }

    ResultScanner resultScanner;
    if (!StringUtils.isEmpty(dataRequest.getEndKey())) {
      RowKey rowKey = RowKeyFactory.newInstance(null, dataRequest.getEndKey()
          .getBytes());
      s.setStartRow(rowKey.getKey());
      resultScanner = table.getScanner(s);
      resultScanner.next();
    } else {
      resultScanner = table.getScanner(s);
    }

    // Populate rows
    int countDisplayRecords = 0;
    int countTotalResults =
        dataRequest.getEndKeyPosition() > 0 ? dataRequest.getEndKeyPosition() :
            dataRequest.getIDisplayStart();
    boolean forward = dataRequest.getEndKeyPosition() > 0;
    long endKeyPosition = dataRequest.getEndKeyPosition();
    for (Result r : resultScanner) {

      //skip to start offset
      if (++endKeyPosition <= dataRequest.getIDisplayStart()) {
        if (forward) {
          ++countTotalResults;
        }
        continue;
      }

      ++countTotalResults;

      if (StringUtils.isEmpty(dataRequest.getSSearch())) {
        //check if we have PAGING_FORWARD_PRESCAN more pages of results (for paging)
        if (countTotalResults >= dataRequest.getIDisplayStart() +
            dataRequest.getIDisplayLength() * PAGING_FORWARD_PRESCAN) break;
      }

      //stop when required number of results is fetched
      if (++countDisplayRecords > dataRequest.getIDisplayLength()) continue;

      SortedMap<Integer, Object> data = new TreeMap<Integer, Object>();
      for (int i=0; i<=16; i++) {
        data.put(i, "missing_column");
      }
      // Sorting order
      // 0 - Key
      // 1 - Name
      // 2 - Directory
      // 3 - Length
      // 4 - blockSize
      // 5 - block
      // 6 - mtime
      // 7 - atime
      // 8 - permissions
      // 9 - userName
      // 10 - groupname
      // 11 - symlink
      // 12 - dsQuota
      // 13 - nsQuota
      // 14 - action
      // 15 - replication
      // 16 - state

      data.put(0, new String(r.getRow()));

      for (FileField entry : FileField.values()) {
        if (r.containsColumn(FileField.FILE_ATTRIBUTES.getBytes(),
            entry.getBytes())) {
          if (entry == FileField.PERMISSIONS) {
            data.put(8, NamespaceProcessor.getPermissions(r).toString());
          } else if (entry == FileField.BLOCK) {
            List<LocatedBlockDescriptor> locatedBlockResult =
                new ArrayList<LocatedBlockDescriptor>();
            List<LocatedBlock> blockArrayList = NamespaceProcessor.getBlocks(r);
            long blockCounter = 0;
            for (LocatedBlock locatedBlock : blockArrayList) {
              if (blockCounter >= BLOCK_RESULT_LIMIT) {
                break;
              }
              locatedBlockResult.add(new LocatedBlockDescriptor(locatedBlock));
              blockCounter++;
            }
            data.put(5, new GiraffaWebJsonWrappers.LocatedBlocksWrapper
                (locatedBlockResult, blockArrayList.size()));
          } else {
            switch (entry) {
              case A_TIME:     data.put(7, NamespaceProcessor.getATime(r));     break;
              case M_TIME:     data.put(6, NamespaceProcessor.getMTime(r));     break;
              case ACTION:     data.put(14, new String(r.getValue(FileField.
                  getFileAttributes(), FileField.getAction())));                break;
              case NAME:       data.put(1, NamespaceProcessor.getFileName(r));  break;
              case NS_QUOTA:   data.put(13, NamespaceProcessor.getNsQuota(r));  break;
              case BLOCK_SIZE: data.put(4, NamespaceProcessor.getBlockSize(r)); break;
              case DIRECTORY:  data.put(2, NamespaceProcessor.getDirectory(r)); break;
              case DS_QUOTA:   data.put(12, NamespaceProcessor.getDsQuota(r));  break;
              case GROUP_NAME: data.put(10, NamespaceProcessor.getGroupName(r));break;
              case LENGTH:     data.put(3, NamespaceProcessor.getLength(r));    break;
              case PERMISSIONS:data.put(8, NamespaceProcessor.getPermissions(r)); break;
              case REPLICATION:data.put(15, NamespaceProcessor.getReplication(r)); break;
              case USER_NAME:  data.put(9, NamespaceProcessor.getUserName(r));  break;
              case SYMLINK:    data.put(11, NamespaceProcessor.getSymlink(r));  break;
              case STATE:      data.put(16, NamespaceProcessor.getState(r));    break;
            }
          }
        }
      }
      dataReply.getAaData().add(data);
    }


    dataReply.setITotalRecords(countTotalResults);
    dataReply.setITotalDisplayRecords(countTotalResults);

    resultScanner.close();

    mapper.writeValue(response.getWriter(), dataReply);
  }


  @Override
  public void destroy() {
    super.destroy();
    try {
      if (table != null) {
        synchronized (table) {
          table.close();
          table = null;
        }
      }
    } catch (IOException e) {
      LOG.error("Cannot close table: ", e);
    }
  }

  private void openTable() {
    if (this.table != null) return;
    MasterCoprocessorEnvironment masterEnv =
        (MasterCoprocessorEnvironment) getServletContext()
            .getAttribute("masterEnvironment");
    String tableName = masterEnv.getConfiguration()
        .get(GiraffaConfiguration.GRFA_TABLE_NAME_KEY,
            GiraffaConfiguration.GRFA_TABLE_NAME_DEFAULT);
    try {
      table = masterEnv.getTable(tableName.getBytes());
    } catch (IOException e) {
      LOG.error("Cannot get table: " + tableName, e);
    }
  }
}