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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import opennlp.tools.entitylinker.GeoEntityLinker;

/**
 * Generates Lists of EntityLinker implementations via
 * properties file configuration
 *
 */
public class EntityLinkerFactory {

  /**
   * Instantiates a list of EntityLinkers based on a properties file entry that
   * consists of a comma separated list of full class names. The entityType is
   * used to build the key to the properties entry. the entityType will be
   * prefixed with "linker." Therefore, a compliant property entry for location
   * entity linker types would be:
   * linker.<yourtype>=<yourclass1,yourclass2>
   * For example:
   * linker.location=opennlp.tools.entitylinker.GeoEntityLinker,opennlp.tools.entitylinker.GeoEntityLinker2
   *
   *
   * @param entityType the type of entity, the same as what would be returned
   *                   from span.getType()
   * @param properties the entitylinker properties that contain the configured entitylinkers
   * @return  
  
   */
  public static synchronized List<EntityLinker> getLinkers(String entityType, EntityLinkerProperties properties) {
    List<EntityLinker> linkers = new ArrayList<EntityLinker>();
    try {
      String listoflinkers = properties.getProperty("linker." + entityType, GeoEntityLinker.class.getName());    
      for (String classname : listoflinkers.split(",")) {
        Class theClass = Class.forName(classname);
        EntityLinker linker = (EntityLinker) theClass.newInstance();
        System.out.println("EntityLinker factory instantiated: " + linker.getClass().getName());
        linkers.add(linker);
      }
    } catch (Exception ex) {
      Logger.getLogger(EntityLinkerFactory.class.getName()).log(Level.SEVERE, null, ex);
    }
    return linkers;
  }

  /**
   *
   * @param entityTypes the types of entities, i.e person, location, organization
     * @param properties the entitylinker properties that contain the configured entitylinkers
   * @return
   */
  public static synchronized List<EntityLinker> getLinkers(String[] entityTypes, EntityLinkerProperties properties) {
    List<EntityLinker> linkers = new ArrayList<EntityLinker>();

    for (String entityType : entityTypes) {
      try {
        String listoflinkers = properties.getProperty("linker." + entityType, GeoEntityLinker.class.getName());   
        for (String classname : listoflinkers.split(",")) {
          Class theClass = Class.forName(classname);
          EntityLinker linker = (EntityLinker) theClass.newInstance();
          System.out.println("EntityLinker factory instantiated: " + linker.getClass().getName());
          linkers.add(linker);
        }
      }  catch (Exception ex) {
        Logger.getLogger(EntityLinkerFactory.class.getName()).log(Level.SEVERE, null, ex);
      }

    }

    return linkers;
  }


}
