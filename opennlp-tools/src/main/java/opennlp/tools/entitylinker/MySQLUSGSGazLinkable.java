/*
 * Copyright 2013 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package opennlp.tools.entitylinker;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import opennlp.tools.entitylinker.EntityLinkerProperties;
import opennlp.tools.entitylinker.domain.BaseLink;
import opennlp.tools.util.Span;

public class MySQLUSGSGazLinkable {

  private Connection con;
  private Boolean filterCountryContext;

  public MySQLUSGSGazLinkable() {
  }

  public ArrayList<BaseLink> find(String locationText, Span span, List<CountryContextHit> countryHits, EntityLinkerProperties properties) {
    ArrayList<BaseLink> returnlocs = new ArrayList<BaseLink>();
    try {
      filterCountryContext = Boolean.valueOf(properties.getProperty("geoentitylinker.filter_by_country_context", "false"));
      //the usgs gazateer only has us geonames, so only use it if the user doesn't care about country isolation or the hits contain us
      if (getCountryCodes(countryHits).contains("us") || !filterCountryContext) {

        if (con == null) {
          con = getMySqlConnection(properties);
        }
        String thresh = properties.getProperty("mysqlusgsgazscorethresh", "10");
        int threshhold = -1;
        if (!thresh.matches("[azAZ]")) {
          threshhold = Integer.valueOf(thresh);
        }
        returnlocs.addAll(this.searchGaz(locationText, threshhold, getCountryCodes(countryHits), properties));
      }
    } catch (Exception ex) {
      Logger.getLogger(MySQLUSGSGazLinkable.class.getName()).log(Level.SEVERE, null, ex);
    }

    return returnlocs;
  }

  protected Connection getMySqlConnection(EntityLinkerProperties properties) throws Exception {
    String driver = properties.getProperty("mysql.driver", "org.gjt.mm.mysql.Driver");
    String url = properties.getProperty("mysql.url", "jdbc:mysql://127.0.0.1:3306/world");
    String username = properties.getProperty("mysql.username", "root");
    String password = properties.getProperty("mysql.password", "559447");

    Class.forName(driver);
    Connection conn = DriverManager.getConnection(url, username, password);
    return conn;
  }

  private ArrayList<MySQLUSGSGazEntry> searchGaz(String searchString, int matchthresh, Set<String> countryCodes, EntityLinkerProperties properties) throws SQLException, Exception {
    if (con.isClosed()) {
      con = getMySqlConnection(properties);
    }
    CallableStatement cs;
    cs = con.prepareCall("CALL `search_gaz`(?, ?)");
    cs.setString(1, this.format(searchString));
    cs.setInt(2, matchthresh);
    ArrayList<MySQLUSGSGazEntry> retUrls = new ArrayList<MySQLUSGSGazEntry>();
    ResultSet rs;
    try {
      rs = cs.executeQuery();

      if (rs == null) {
        return retUrls;
      }

      while (rs.next()) {
        MySQLUSGSGazEntry s = new MySQLUSGSGazEntry();
        s.setRank(rs.getDouble(1));

        s.setFeatureid(String.valueOf(rs.getLong(2)));
        s.setFeaturename(rs.getString(3));
        s.setFeatureclass(rs.getString(4));
        s.setStatealpha(rs.getString(5));
        s.setPrimarylatitudeDEC(rs.getDouble(6));
        s.setPrimarylongitudeDEC(rs.getDouble(7));
        s.setMapname(rs.getString(8));
        if (countryCodes.contains("us")) {
          s.setRank(s.getRank() + 1.0);
          System.out.println("qualified on: US");
        }
        retUrls.add(s);
      }

    } catch (SQLException ex) {
      throw ex;
    } catch (Exception e) {
      System.err.println(e);
    } finally {
      con.close();
    }

    return retUrls;
  }

  private Set<String> getCountryCodes(List<CountryContextHit> hits) {
    Set<String> ccs = new HashSet<String>();
    for (CountryContextHit hit : hits) {
      ccs.add(hit.getCountryCode().toLowerCase());
    }
    return ccs;
  }

  public String format(String entity) {
    return "\"" + entity + "\"";
  }
}
