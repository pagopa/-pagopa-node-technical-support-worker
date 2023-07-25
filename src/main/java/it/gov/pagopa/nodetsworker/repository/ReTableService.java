package it.gov.pagopa.nodetsworker.repository;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableServiceClient;
import com.azure.data.tables.TableServiceClientBuilder;
import com.azure.data.tables.models.ListEntitiesOptions;
import com.azure.data.tables.models.TableEntity;
import it.gov.pagopa.nodetsworker.repository.model.EventEntity;
import it.gov.pagopa.nodetsworker.util.Util;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.inject.ConfigProperty;

// @Startup
@ApplicationScoped
// @UnlessBuildProfile("test")
public class ReTableService {

  @ConfigProperty(name = "re-table-storage.connection-string")
  String connString;

  @ConfigProperty(name = "re-table-storage.table-name")
  String tableName;

  private TableServiceClient tableServiceClient = null;

  public TableClient getTableClient() {
    if (tableServiceClient == null) {
      tableServiceClient =
          new TableServiceClientBuilder().connectionString(connString).buildClient();
      tableServiceClient.createTableIfNotExists(tableName);
    }
    return tableServiceClient.getTableClient(tableName);
  }

  private List<String> propertiesToSelect =
      Arrays.asList(
          "serviceIdentifier",
          "status",
          "psp",
          "canale",
          "noticeNumber",
          "paymentToken",
          "idDominio",
          "iuv",
          "ccp",
          "insertedTimestamp");

  private EventEntity tableEntityToEventEntity(TableEntity e) {
    EventEntity ee = new EventEntity();
        ee.setCanale(getString(e.getProperty("canale")));
        ee.setIuv(getString(e.getProperty("iuv")));
        ee.setCcp(getString(e.getProperty("ccp")));
        ee.setNoticeNumber(getString(e.getProperty("noticeNumber")));
        ee.setPaymentToken(getString(e.getProperty("paymentToken")));
        ee.setIdDominio(getString(e.getProperty("idDominio")));
        ee.setServiceIdentifier(getString(e.getProperty("serviceIdentifier")));
        ee.setInsertedTimestamp(getString(e.getProperty("insertedTimestamp")));
        ee.setPsp(getString(e.getProperty("psp")));
        ee.setStatus(getString(e.getProperty("status")));
        ee.setUniqueId(getString(e.getProperty("uniqueId")));
    return ee;
  }

  public List<EventEntity> findReByCiAndNN(
      LocalDate datefrom, LocalDate dateTo, String creditorInstitution, String noticeNumber) {

    String filter =
        String.format(
            "PartitionKey ge '%s' and PartitionKey le '%s' and idDominio eq '%s' and noticeNumber"
                + " eq '%s' and esito eq 'CAMBIO_STATO'",
            Util.format(datefrom), Util.format(dateTo), creditorInstitution, noticeNumber);
    ListEntitiesOptions options =
        new ListEntitiesOptions().setFilter(filter).setSelect(propertiesToSelect);
    return getTableClient().listEntities(options, null, null).stream()
        .map(
            e -> {
              return tableEntityToEventEntity(e);
            })
        .collect(Collectors.toList());
  }

  public List<EventEntity> findReByCiAndIUV(
      LocalDate datefrom, LocalDate dateTo, String creditorInstitution, String iuv) {
    ListEntitiesOptions options =
        new ListEntitiesOptions()
            .setFilter(
                String.format(
                    "PartitionKey ge '%s' and PartitionKey le '%s' and idDominio eq '%s' and iuv eq"
                        + " '%s' and esito eq 'CAMBIO_STATO'",
                    Util.format(datefrom), Util.format(dateTo), creditorInstitution, iuv))
            .setSelect(propertiesToSelect);
    return getTableClient().listEntities(options, null, null).stream()
        .map(
            e -> {
              return tableEntityToEventEntity(e);
            })
        .collect(Collectors.toList());
  }

  public List<EventEntity> findReByCiAndNNAndToken(
      LocalDate datefrom,
      LocalDate dateTo,
      String creditorInstitution,
      String noticeNumber,
      String paymentToken) {
    ListEntitiesOptions options =
        new ListEntitiesOptions()
            .setFilter(
                String.format(
                    "PartitionKey ge '%s' and PartitionKey le '%s' and idDominio eq '%s' and"
                        + " noticeNumber eq '%s' and paymentToken eq '%s' and esito eq"
                        + " 'CAMBIO_STATO'",
                    Util.format(datefrom),
                    Util.format(dateTo),
                    creditorInstitution,
                    noticeNumber,
                    paymentToken))
            .setSelect(propertiesToSelect);
    return getTableClient().listEntities(options, null, null).stream()
        .map(
            e -> {
              return tableEntityToEventEntity(e);
            })
        .collect(Collectors.toList());
  }

  public List<EventEntity> findReByCiAndIUVAndCCP(
      LocalDate datefrom, LocalDate dateTo, String creditorInstitution, String iuv, String ccp) {
    ListEntitiesOptions options =
        new ListEntitiesOptions()
            .setFilter(
                String.format(
                    "PartitionKey ge '%s' and PartitionKey le '%s' and idDominio eq '%s' and iuv eq '%s'"
                        + " and ccp eq '%s' and esito eq 'CAMBIO_STATO'",
                    Util.format(datefrom), Util.format(dateTo), creditorInstitution, iuv, ccp))
            .setSelect(propertiesToSelect);
    return getTableClient().listEntities(options, null, null).stream()
        .map(
            e -> {
              return tableEntityToEventEntity(e);
            })
        .collect(Collectors.toList());
  }

  public List<EventEntity> findReByPartitionAndRow(
          String partitionKey, String rowId
  ) {
    ListEntitiesOptions options =
            new ListEntitiesOptions()
                    .setFilter(String.format("PartitionKey eq '%s' and RowKey eq '%s'", partitionKey,rowId))
                    .setSelect(propertiesToSelect);
    return getTableClient().listEntities(options, null, null).stream()
            .map(
                    e -> {
                      return tableEntityToEventEntity(e);
                    })
            .collect(Collectors.toList());
  }

  private String getString(Object o) {
    if (o == null) return null;
    return (String) o;
  }
}
