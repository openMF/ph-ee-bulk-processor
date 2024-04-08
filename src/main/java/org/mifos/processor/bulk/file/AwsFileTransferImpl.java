package org.mifos.processor.bulk.file;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@Qualifier("awsStorage")
public class AwsFileTransferImpl implements FileTransferService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private AmazonS3 s3Client;

    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String accessSecret;
    @Value("${cloud.aws.region.static}")
    private String region;
    @Value("${cloud.aws.s3BaseUrl}")
    private String endpoint;

    @Override
    public byte[] downloadFile(String fileName, String bucketName) {

        AWSCredentials credentials = new BasicAWSCredentials(accessKey, accessSecret);
        s3Client = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withPathStyleAccessEnabled(true).withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                .build();

        AmazonS3ClientBuilder s3ClientBuilder = AmazonS3ClientBuilder.standard();

        s3ClientBuilder.setEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region) // Set
                                                                                                              // your
                                                                                                              // desired
                                                                                                              // region
                                                                                                              // here
        );
        logger.info("________________________> {}", endpoint);
        logger.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$> 1 {}", s3ClientBuilder.getEndpoint().getServiceEndpoint());
        logger.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$> 2 {}", s3ClientBuilder.getRegion());
        logger.info("$$$$$$$$$$$$$$$$$$$$$$$$$$$$> 3 {}", s3ClientBuilder.getClientConfiguration());

        S3Object s3Object = s3Client.getObject(bucketName, fileName);
        S3ObjectInputStream inputStream = s3Object.getObjectContent();
        try {
            byte[] content = IOUtils.toByteArray(inputStream);
            return content;
        } catch (IOException e) {
            logger.debug("{}", e.getMessage());
        }
        return null;
    }

    @Override
    public String uploadFile(MultipartFile file, String bucketName) {

        File fileObj = convertMultiPartFileToFile(file);
        return uploadFile(fileObj, bucketName);
    }

    @Override
    public String uploadFile(File file, String bucketName) {
        String fileName = file.getName();
        s3Client.putObject(new PutObjectRequest(bucketName, fileName, file));
        file.delete();

        return fileName;
    }

    @Override
    public InputStream streamFile(String fileName, String bucketName) {
        S3Object s3Object = s3Client.getObject(bucketName, fileName);
        S3ObjectInputStream inputStream = s3Object.getObjectContent();
        return inputStream;
    }

    @Override
    public void deleteFile(String fileName, String bucketName) {
        s3Client.deleteObject(bucketName, fileName);
    }

    private File convertMultiPartFileToFile(MultipartFile file) {
        File convertedFile = new File(file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(convertedFile)) {
            fos.write(file.getBytes());
        } catch (IOException e) {
            logger.error("Error converting multipartFile to file", e);
        }
        return convertedFile;
    }
}
