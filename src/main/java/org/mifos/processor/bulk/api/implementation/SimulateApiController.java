package org.mifos.processor.bulk.api.implementation;

import javax.servlet.http.HttpServletResponse;
import org.mifos.processor.bulk.api.definition.Simulate;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimulateApiController implements Simulate {

    @Override
    public void simulate(HttpServletResponse httpServletResponse) {
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
    }
}
