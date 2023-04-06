package org.mifos.processor.bulk.API;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.mifos.processor.bulk.utility.Headers;
import org.mifos.processor.bulk.utility.SpringWrapperUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
@RestController
public class SendCallBackController implements SendCallBack{

    @Autowired
    private ProducerTemplate producerTemplate;
    @Override
    public Object callBack() throws IOException {
        Headers headers = new Headers.HeaderBuilder()
                .build();
        Exchange exchange = SpringWrapperUtil.getDefaultWrappedExchange(producerTemplate.getCamelContext(),
                headers, null);
        producerTemplate.send("direct:send-Callback", exchange);
        return Object.class;
    }
}
