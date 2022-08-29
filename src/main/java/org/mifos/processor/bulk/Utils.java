package org.mifos.processor.bulk;

public class Utils {

    public static String getTenantSpecificWorkflowId(String originalWorkflowName, String tenantName) {
        return originalWorkflowName.replace("{dfspid}", tenantName);
    }

}
