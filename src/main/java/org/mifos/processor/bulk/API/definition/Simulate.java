package org.mifos.processor.bulk.api.definition;

import org.springframework.web.bind.annotation.PostMapping;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

// from("rest:post:/simulate").log("Reached Simulation");
public interface Simulate {

    @PostMapping(value = "/simulate", produces="application/json")
    void simulate(HttpServletResponse httpServletResponse) throws IOException;

}
