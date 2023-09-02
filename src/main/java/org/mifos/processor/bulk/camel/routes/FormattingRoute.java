package org.mifos.processor.bulk.camel.routes;

import static org.mifos.processor.bulk.camel.config.CamelProperties.OVERRIDE_HEADER;
import static org.mifos.processor.bulk.camel.config.CamelProperties.TRANSACTION_LIST;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FORMATTING_FAILED;
import static org.mifos.processor.bulk.zeebe.ZeebeVariables.FORMATTING_STANDARD;

import java.util.List;
import org.mifos.processor.bulk.format.Standard;
import org.mifos.processor.bulk.format.helper.Mappers;
import org.mifos.processor.bulk.schema.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FormattingRoute extends BaseRouteBuilder {

    @Autowired
    private Mappers mappers;

    @Value("${config.formatting.standard}")
    private String standard;

    private Standard formattingStandard;

    @Override
    public void configure() throws Exception {
        // parsing enum from application.yaml string
        formattingStandard = Standard.valueOf(standard);

        /**
         * Base route for kicking off ordering logic. Performs below tasks. 1. Downloads the csv form cloud. 2. Builds
         * the [Transaction] array using [direct:get-transaction-array] route. 3. Format the data based on the
         * configuration provided in application.yaml. @see [Standard.java] 4. Update file with the updated data. 5.
         * Uploads the updated file in cloud.
         */
        from(RouteId.FORMATTING.getValue()).id(RouteId.FORMATTING.getValue()).log("Starting route " + RouteId.FORMATTING.name()).choice()
                .when(exchange -> formattingStandard != Standard.DEFAULT).to("direct:download-file").to("direct:get-transaction-array")
                .to("direct:format-data")
                // making sure to override header as well, since data format is now updated
                .process(exchange -> exchange.setProperty(OVERRIDE_HEADER, true)).to("direct:update-file").to("direct:upload-file")
                .otherwise().log("Skipping formatting since standard is set to DEFAULT").end().process(exchange -> {
                    exchange.setProperty(FORMATTING_FAILED, false);
                    exchange.setProperty(FORMATTING_STANDARD, standard);
                });

        // formatting data based on configuration. Uses [Mappers] for converting data.
        from("direct:format-data").id("direct:format-data").log("Starting route direct:format-data").process(exchange -> {
            List<Transaction> transactionList = exchange.getProperty(TRANSACTION_LIST, List.class);
            // replace with switch when multiple standards are added
            if (formattingStandard == Standard.GSMA) {
                logger.info("Formatting based on {} standard", formattingStandard.name());
                exchange.setProperty(TRANSACTION_LIST, mappers.gsmaMapper.convertList(transactionList));
            } else {
                exchange.setProperty(TRANSACTION_LIST, transactionList);
            }
        });
    }

}
