package org.mifos.processor.bulk.api.definition;

import java.io.IOException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PostMapping;

// from("rest:post:/simulate").log("Reached Simulation");
public interface Simulate {

    @PostMapping(value = "/simulate", produces = "application/json")
    void simulate(HttpServletResponse httpServletResponse) throws IOException;

}
