package integration.org.rapidpm.service.persistence;

import org.junit.Before;
import org.junit.Test;
import org.rapidpm.ddi.DI;
import org.rapidpm.service.persistence.UsdaDAO;

import javax.inject.Inject;
import java.sql.Connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class UsdaDAOTest001 {

  @Inject
  private UsdaDAO usdaDAO;

  @Before
  public void setUp() throws Exception {
    DI.clearReflectionModel();
    DI.activateDI(this);
    DI.activateDI("org.rapidpm");
  }

  @Test
  public void test001() throws Exception {
    final Connection connection = usdaDAO.getConnection();
    assertFalse(connection.isClosed());
    connection.close();
  }

  @Test
  public void test002() throws Exception {
    final int countOfFoodGroups = usdaDAO.getCountOfFoodGroups();
    assertEquals(24, countOfFoodGroups);
  }
}
