package org.mifos.processor.bulk.camel.config;

public class CamelProperties {

    private CamelProperties() {}

    public static final String AUTH_TYPE = "authType";
    public static final String IS_BATCH_READY = "isBatchReady"; // camel property to check if batch is ready for sampling

    public static final String SERVER_FILE_NAME = "serverFileNam";

    public static final String LOCAL_FILE_PATH = "localFilePath";

}
