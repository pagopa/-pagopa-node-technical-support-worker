package it.gov.pagopa.nodetsworker.repository;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedIterable;
import io.quarkus.runtime.Startup;
import it.gov.pagopa.nodetsworker.repository.model.VerifyKOEvent;
import it.gov.pagopa.nodetsworker.util.Util;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ApplicationScoped
@Startup
public class CosmosVerifyKOEventClient {

  @ConfigProperty(name = "verifyko.endpoint")
  private String endpoint;

  @ConfigProperty(name = "verifyko.key")
  private String key;

  public static String dbname = "nodo_verifyko";
  public static String tablename = "events";

  private CosmosClient client;

  @Inject Logger log;

  private String dateFilter = " and c.faultBean.dateTime > @from and c.faultBean.dateTime < @to";

  private CosmosClient getClient() {
    if (client == null) {
      client = new CosmosClientBuilder().endpoint(endpoint).key(key).buildClient();
    }
    return client;
  }

  private CosmosPagedIterable<VerifyKOEvent> query(SqlQuerySpec query) {
    log.info("executing query:" + query.getQueryText());
    CosmosContainer container = getClient().getDatabase(dbname).getContainer(tablename);
    return container.queryItems(query, new CosmosQueryRequestOptions(), VerifyKOEvent.class);
  }

  public CosmosPagedIterable<VerifyKOEvent> findEventsByCiAndNN(
      String organizationFiscalCode,
      String noticeNumber,
      LocalDate dateFrom,
      LocalDate dateTo) {
    List<SqlParameter> paramList = new ArrayList<>();
    paramList.addAll(Arrays.asList(
            new SqlParameter("@organizationFiscalCode", organizationFiscalCode),
            new SqlParameter("@noticeNumber", noticeNumber),
            new SqlParameter("@from", Util.format(dateFrom)),
            new SqlParameter("@to", Util.format(dateTo.plusDays(1)))
        ));
    SqlQuerySpec q =
        new SqlQuerySpec(
                "SELECT * FROM c where"
                    + " c.creditor.idPA = @organizationFiscalCode"
                    + " and c.debtorPosition.noticeNumber = @noticeNumber"
                    + dateFilter
        )
            .setParameters(paramList);
    return query(q);
  }

}
