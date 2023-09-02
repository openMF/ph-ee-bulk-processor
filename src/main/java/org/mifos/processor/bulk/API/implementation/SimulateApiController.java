package org.mifos.processor.bulk.api.implementation;

import org.mifos.processor.bulk.api.definition.Simulate;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletResponse;

@RestController
public class SimulateApiController implements Simulate {

    @Override
    public void simulate(HttpServletResponse httpServletResponse) {
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
    }
}
