package org.mifos.processor.bulk.API;

import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;

public interface SendCallBack {
    @GetMapping(value = "test/send/callback")
    Object callBack() throws IOException;
}
