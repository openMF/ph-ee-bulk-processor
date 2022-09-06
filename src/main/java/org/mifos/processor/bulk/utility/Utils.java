package org.mifos.processor.bulk.utility;

import java.io.*;

public class Utils {

    public static String getTenantSpecificWorkflowId(String originalWorkflowName, String tenantName) {
        return originalWorkflowName.replace("{dfspid}", tenantName);
    }

    public static String mergeCsvFile(String file1, String file2) {
        try {
            // create a writer for permFile
            BufferedWriter out = new BufferedWriter(new FileWriter(file1, true));
            // create a reader for tmpFile
            BufferedReader in = new BufferedReader(new FileReader(file2));
            String str;
            int i = 0;
            while ((str = in.readLine()) != null) {
                if (i == 0) {
                    i = 1;
                    continue;
                }
                out.write(str+"\n");
            }
            in.close();
            out.close();
        } catch (IOException e) {
            return null;
        }

        return file1;
    }

}
