package org.rapidpm.service.rest;

import org.rapidpm.service.persistence.UsdaDAO;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.sql.SQLException;

@Path("/usda")
public class UsdaRestService {

  @Inject
  UsdaDAO usdaDAO;


  @GET()
  @Path("getCountOfFoodGroups")
  @Produces("text/plain")
  public String getCountOfFoodGroups(){
    try {
      return "" + usdaDAO.getCountOfFoodGroups();
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return "ERROR";
  }


}
