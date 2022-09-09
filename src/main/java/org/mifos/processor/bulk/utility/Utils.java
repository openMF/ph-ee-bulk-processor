package org.mifos.processor.bulk.utility;

import javax.net.ssl.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

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
            boolean isFirstLine = true;
            while ((str = in.readLine()) != null) {
                if (isFirstLine) {
                    // used for skipping header writing
                    isFirstLine = false;
                    continue;
                }
                out.write(str+"\n");
            }
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return file1;
    }

    public static String getAwsFileUrl(String baseUrl, String filename) {
        return String.format("%s/%s", baseUrl, filename);
    }

    /**
     * takes initial timer in the ISO 8601 durations format
     * for more info check
     * https://docs.camunda.io/docs/0.26/reference/bpmn-workflows/timer-events/#time-duration
     *
     * @param initialTimer initial timer in the ISO 8601 durations format, ex: PT45S
     * @return next timer value in the ISO 8601 durations format
     */
    public static String getNextTimer(String initialTimer){
        String stringSecondsValue = initialTimer.split("T")[1].split("S")[0];
        int initialSeconds = Integer.parseInt(stringSecondsValue);

        int currentPower = (int) ( Math.log(initialSeconds) / Math.log(2) );
        int next = (int) Math.pow(2, ++currentPower);

        return String.format("PT%sS", next);
    }

    public static String getZeebeTimerValue(int timer) {
        return String.format("PT%sS", timer);
    }

}
