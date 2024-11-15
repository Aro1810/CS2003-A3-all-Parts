/*
  Using java.util.Properties to demonstrate configuration information
  
  Based on code from Saleem Bhatti
  Sep 2019
  Oct 2018

*/

import java.io.*;
import java.net.*;

// https://docs.oracle.com/en/java/javase/23/docs/api/java.base/java/util/Properties.html
import java.util.Properties;

public class Configuration
{
  public Properties    properties_;
  public String        propertiesFile_ = "cs2003-C3.properties";

  // default values can be overriden in the properties file.
  public int        serverPort_=12345; // A default port value
  public String     documentRoot_;
  public String     federation_;

  Configuration(String propertiesFile)
  {
    if (propertiesFile != null) {
        propertiesFile_ = propertiesFile;
    }

    try {
      properties_ = new Properties();
      InputStream p = getClass().getClassLoader().getResourceAsStream(propertiesFile_);
      if (p != null) {
        properties_.load(p);
        String s;

        if ((s = properties_.getProperty("serverPort")) != null){
          System.out.println(propertiesFile_ + " serverPort: " + serverPort_ + " -> " + s);
          serverPort_ = Integer.parseInt(s);
        }
        
        if ((s = properties_.getProperty("documentRoot")) != null){
          System.out.println(propertiesFile_ + " documentRoot: " + documentRoot_ + " -> " + s);
          documentRoot_ = new String(s);
        }

        if ((s = properties_.getProperty("federation")) != null){
          System.out.println(propertiesFile_ + " federation: " + federation_ + " -> " + s);
          federation_ = new String(s);
        }

        p.close();
      }

    }

    catch (NumberFormatException e) {
      System.out.println("Problem: " + e.getMessage());
    }

    catch (IOException e) {
      System.out.println("Problem: " + e.getMessage());
    }

  }
}
