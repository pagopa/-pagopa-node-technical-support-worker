package it.gov.pagopa.nodetsworker.resources;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableServiceClient;
import com.azure.data.tables.TableServiceClientBuilder;
import io.quarkiverse.mockserver.test.MockServerTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.common.mapper.TypeRef;
import it.gov.pagopa.nodetsworker.models.PaymentInfo;
import it.gov.pagopa.nodetsworker.models.TransactionResponse;
import it.gov.pagopa.nodetsworker.repository.CosmosBizEventClient;
import it.gov.pagopa.nodetsworker.repository.CosmosNegBizEventClient;
import it.gov.pagopa.nodetsworker.util.AppConstantTestHelper;
import it.gov.pagopa.nodetsworker.util.AzuriteResource;
import it.gov.pagopa.nodetsworker.util.CosmosResource;
import lombok.SneakyThrows;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static io.restassured.RestAssured.given;
import static it.gov.pagopa.nodetsworker.util.AppConstantTestHelper.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

@QuarkusTest
@QuarkusTestResource(MockServerTestResource.class)
@QuarkusTestResource(AzuriteResource.class)
@QuarkusTestResource(CosmosResource.class)
class ResourceTest {

  @ConfigProperty(name = "re-table-storage.connection-string")
  String connString;

  @ConfigProperty(name = "biz.endpoint")
  String bizendpoint;

  @ConfigProperty(name = "biz.key")
  String bizkey;

  private TableClient tableClient;
  private CosmosClient clientbiz;

  private TableClient getTableClient(){
    if(tableClient==null){
      TableServiceClient tableServiceClient = new TableServiceClientBuilder().connectionString(connString).buildClient();
      tableServiceClient.createTableIfNotExists("events");
      tableClient = tableServiceClient.getTableClient("events");
    }
    return tableClient;
  }

  private CosmosClient getCosmosClient(){
    if(clientbiz==null) {
      clientbiz = new CosmosClientBuilder().endpoint(bizendpoint).key(bizkey).buildClient();
      clientbiz.createDatabaseIfNotExists(CosmosBizEventClient.dbname);
      clientbiz.getDatabase(CosmosBizEventClient.dbname).createContainerIfNotExists(CosmosBizEventClient.tablename,"/timestamp");
      clientbiz.getDatabase(CosmosBizEventClient.dbname).createContainerIfNotExists(CosmosNegBizEventClient.tablename,"/timestamp");
    }
    return clientbiz;

  }

  @SneakyThrows
  @Test
  @DisplayName("by ci and nn with positive")
  void test1() {
    String noticeNumber = String.valueOf(new Random().nextLong(11111111111l, 99999999999l));
    String url = SP03_NN.formatted(PA_CODE, noticeNumber);

    getTableClient().createEntity(AppConstantTestHelper.newRe(PA_CODE, noticeNumber,null));
    getCosmosClient().getDatabase(CosmosBizEventClient.dbname).getContainer(CosmosBizEventClient.tablename).createItem(
        AppConstantTestHelper.newPositiveBiz(PA_CODE, noticeNumber,null),new CosmosItemRequestOptions()
    );

    TransactionResponse res = given()
        .when()
        .get(url)
        .then()
        .statusCode(200)
        .extract()
        .as(new TypeRef<TransactionResponse<PaymentInfo>>() {});
    assertThat(res.getPayments().size(), greaterThan(0));
    PaymentInfo o = (PaymentInfo)res.getPayments().get(0);
    assertThat(o.getNoticeNumber(),equalTo(noticeNumber));
    assertThat(o.getOrganizationFiscalCode(),equalTo(PA_CODE));
    assertThat(o.getOutcome(),equalTo(AppConstantTestHelper.outcomeOK));
    assertThat(o.getPspId(),equalTo("pspTest"));
    assertThat(o.getChannelId(),equalTo("canaleTest"));
    assertThat(o.getBrokerPspId(),equalTo("intTest"));
  }

  @SneakyThrows
  @Test
  @DisplayName("by ci and nn with negative")
  void test2() {
    String noticeNumber = String.valueOf(new Random().nextLong(11111111111l, 99999999999l));
    String url = SP03_NN.formatted(PA_CODE, noticeNumber);

    getTableClient().createEntity(AppConstantTestHelper.newRe(PA_CODE, noticeNumber,null));
    getCosmosClient().getDatabase(CosmosBizEventClient.dbname).getContainer(CosmosNegBizEventClient.tablename).createItem(
            AppConstantTestHelper.newNegBiz(PA_CODE, noticeNumber,null,false),new CosmosItemRequestOptions()
    );

    TransactionResponse res = given()
            .when()
            .get(url)
            .then()
            .statusCode(200)
            .extract()
            .as(new TypeRef<TransactionResponse<PaymentInfo>>() {});
    assertThat(res.getPayments().size(), greaterThan(0));
    PaymentInfo o = (PaymentInfo)res.getPayments().get(0);
    assertThat(o.getNoticeNumber(),equalTo(noticeNumber));
    assertThat(o.getOrganizationFiscalCode(),equalTo(PA_CODE));
    assertThat(o.getOutcome(),equalTo(AppConstantTestHelper.outcomeKO));
    assertThat(o.getPspId(),equalTo("pspTest"));
    assertThat(o.getChannelId(),equalTo("canaleTest"));
    assertThat(o.getBrokerPspId(),equalTo("intTest"));
  }

  @SneakyThrows
  @Test
  @DisplayName("by ci and iuv with positive")
  void test3() {
    String iuv = String.valueOf(new Random().nextLong(11111111111l, 99999999999l));
    String url = SP03_IUV.formatted(PA_CODE, iuv);

    getTableClient().createEntity(AppConstantTestHelper.newRe(PA_CODE, null,iuv));
    getCosmosClient().getDatabase(CosmosBizEventClient.dbname).getContainer(CosmosBizEventClient.tablename).createItem(
            AppConstantTestHelper.newPositiveBiz(PA_CODE, null, iuv),new CosmosItemRequestOptions()
    );

    TransactionResponse res = given()
            .when()
            .get(url)
            .then()
            .statusCode(200)
            .extract()
            .as(new TypeRef<TransactionResponse<PaymentInfo>>() {});
    assertThat(res.getPayments().size(), greaterThan(0));
    PaymentInfo o = (PaymentInfo)res.getPayments().get(0);
    assertThat(o.getIuv(),equalTo(iuv));
    assertThat(o.getOrganizationFiscalCode(),equalTo(PA_CODE));
    assertThat(o.getOutcome(),equalTo(AppConstantTestHelper.outcomeOK));
    assertThat(o.getPspId(),equalTo("pspTest"));
    assertThat(o.getChannelId(),equalTo("canaleTest"));
    assertThat(o.getBrokerPspId(),equalTo("intTest"));
  }

  @SneakyThrows
  @Test
  @DisplayName("by ci and iuv with negative")
  void test4() {
    String iuv = String.valueOf(new Random().nextLong(11111111111l, 99999999999l));
    String url = SP03_IUV.formatted(PA_CODE, iuv);

    getTableClient().createEntity(AppConstantTestHelper.newRe(PA_CODE, null,iuv));
    getCosmosClient().getDatabase(CosmosBizEventClient.dbname).getContainer(CosmosNegBizEventClient.tablename).createItem(
            AppConstantTestHelper.newNegBiz(PA_CODE, null,iuv,false),new CosmosItemRequestOptions()
    );

    TransactionResponse res = given()
            .when()
            .get(url)
            .then()
            .statusCode(200)
            .extract()
            .as(new TypeRef<TransactionResponse<PaymentInfo>>() {});
    assertThat(res.getPayments().size(), greaterThan(0));
    PaymentInfo o = (PaymentInfo)res.getPayments().get(0);
    assertThat(o.getIuv(),equalTo(iuv));
    assertThat(o.getOrganizationFiscalCode(),equalTo(PA_CODE));
    assertThat(o.getOutcome(),equalTo(AppConstantTestHelper.outcomeKO));
    assertThat(o.getPspId(),equalTo("pspTest"));
    assertThat(o.getChannelId(),equalTo("canaleTest"));
    assertThat(o.getBrokerPspId(),equalTo("intTest"));
  }

}
