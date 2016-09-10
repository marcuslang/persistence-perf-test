package integration.org.rapidpm.service.rest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.rapidpm.microservice.Main;
import org.rapidpm.microservice.test.RestUtils;
import org.rapidpm.service.rest.UsdaRestService;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

public class UsdaRestServiceTest001 {

  @Before
  public void setUp() throws Exception {
    Main.deploy();
  }

  @After
  public void tearDown() throws Exception {
    Main.stop();
  }

  @Test
  public void test001() throws Exception {
    Client client = ClientBuilder.newClient();
    final String uri = new RestUtils().generateBasicReqURL(UsdaRestService.class, Main.CONTEXT_PATH_REST) + "/getCountOfFoodGroups";
    System.out.println("uri = " + uri);
    final int count = client.target(uri)
        .request()
        .get(int.class);
    Assert.assertEquals(24, count);
  }
}
