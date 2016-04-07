/*
 * Copyright 2012 The DC4Cities author.
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

package eu.dc4cities.benchcs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import eu.dc4cities.easc.EASC;
import eu.dc4cities.easc.configuration.Configuration;
import eu.dc4cities.easc.energyservice.EnergyService;
import eu.dc4cities.easc.monitoring.MockMonitor;

public class EASCReader {
    
    public static List<EASC> getEASCs(String configDir, Configuration config, EnergyService energyService) {
        
        System.out.println("Reading EASCs");
        List<EASC> EASCs = new ArrayList<EASC>();
                        
        File dir = new File(configDir);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
          for (File child : directoryListing) {             
              if(child.isDirectory()) {

                  String configDirectory = child.getPath();
                  
                  //read general configuration
                  //Configuration config = ConfigReader.readConfiguration(configDirectory).get();

                  EASC easc = new EASC(configDir, new MockMonitor());
                  
                  EASCs.add(easc);    
              }
          }
        }
        return EASCs;
    }
    
}

