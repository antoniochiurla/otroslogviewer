package pl.otros.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OtrosWebUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(OtrosWebUtils.class.getName());

  public static void checkAvailablePort(String address, int port) {
	  LOGGER.trace("Checking for addr: {} port: {} available", address, port);
  }

}
