package org.mifos.processor.bulk.utility;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultExchange;
import org.springframework.web.multipart.MultipartFile;

public class SpringWrapperUtil {
    public static Exchange getDefaultWrappedExchange(CamelContext camelContext,
                                                     Headers headers) {
        Exchange exchange = new DefaultExchange(camelContext);

        // Setting headers
        for (String headerKey : headers.getHeadersKey()) {
            exchange.getIn().setHeader(headerKey, headers.get(headerKey));
        }

        return exchange;
    }
}
