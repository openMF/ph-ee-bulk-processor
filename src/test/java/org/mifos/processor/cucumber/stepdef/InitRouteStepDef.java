package org.mifos.processor.cucumber.stepdef;

import static com.google.common.truth.Truth.assertThat;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TRANSACTION_LIST_ELEMENT;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.PAYMENT_MODE;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.UUID;
import org.mifos.connector.common.channel.dto.TransactionChannelRequestDTO;
import org.mifos.connector.common.gsma.dto.GSMATransaction;
import org.mifos.processor.bulk.schema.Transaction;

public class InitRouteStepDef extends BaseStepDef {

    @Given("I can load camel context")
    public void loadCamelContext() {
        context.start();
        assertThat(template).isNotNull();
        assertThat(context).isNotNull();
    }

    @When("I call the payment-mode-validation route with {string} payment mode")
    public void callPaymentModeValidationRoute(String paymentMode) {
        exchange = template.send("direct:validate-payment-mode", exchange -> {
            exchange.setProperty(PAYMENT_MODE, paymentMode);
        });
    }

    @Then("I should get a non null exchange variable")
    public void exchangeVariableNullCheck() {
        assertThat(exchange).isNotNull();
    }

    @And("{string} exchange variable should be {string}")
    public void exchangeVariableBooleanCheck(String variableKey, String variableValue) {
        if (variableValue.equalsIgnoreCase("true")) {
            assertThat(exchange.getProperty(variableKey, Boolean.class)).isTrue();
        } else {
            assertThat(exchange.getProperty(variableKey, Boolean.class)).isFalse();
        }
    }

    @When("I call the runtime-payload route with {string} payment mode")
    public void callRuntimePayloadTestRoute(String paymentMode) {
        exchange = template.send("direct:dynamic-payload-setter", exchange -> {
            exchange.setProperty(PAYMENT_MODE, paymentMode);
            exchange.setProperty(TRANSACTION_LIST_ELEMENT, new Transaction() {

                {
                    setId(0);
                    setRequestId(UUID.randomUUID().toString());
                    setPaymentMode(paymentMode);
                    setAmount("100");
                    setPayerIdentifierType("MSISDN");
                    setPayeeIdentifierType("MSISDN");
                    setPayerIdentifier("1234567890");
                    setPayeeIdentifier("0987654321");
                    setCurrency("INR");
                }
            });
        });
    }

    @And("The body should be of GSMA parcelable")
    public void gsmaBodyDeserializeCheck() {
        String body = exchange.getIn().getBody(String.class);
        assertThat(body).isNotNull();

        GSMATransaction gsmaTransaction;
        try {
            gsmaTransaction = objectMapper.readValue(body, GSMATransaction.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        assertThat(gsmaTransaction).isNotNull();
    }

    @And("The body should be of MOJALOOP parcelable")
    public void mojaloopBodyDeserializeCheck() {
        String body = exchange.getIn().getBody(String.class);
        assertThat(body).isNotNull();

        TransactionChannelRequestDTO payload;
        try {
            payload = objectMapper.readValue(body, TransactionChannelRequestDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        assertThat(payload).isNotNull();
    }
}
