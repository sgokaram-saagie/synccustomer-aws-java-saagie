package com.saagie;

import java.io.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import org.json.JSONException;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.core.sync.RequestBody;




public class Customer {


    public static void main(String[] args) throws IOException, JSONException {

        String filename = "customers.json";
        S3Client s3;
        String bucket = "saagiedemo-customer360";
        String folder = "customer/input";


        if (args.length >= 1) {
            filename = args[0];
        }

        System.out.println("Checking for Environment Specific Information");
/*
        if ( System.getenv("AWS_ACCESS_KEY_ID") == null ){
            System.out.println("AWS_ACCESS_KEY_ID not defined. Exiting");
            System.exit(1);

        }

        if ( System.getenv("AWS_SECRET_ACCESS_KEY") == null ){
            System.out.println("AWS_SECRET_ACCESS_KEY not defined. Exiting");
            System.exit(1);

        }
*/
        if ( System.getenv("CUSTOMER_S3_BUCKETNAME") == null ){
            System.out.println("CUSTOMER_S3_BUCKETNAME not defined. Using default value "+bucket);
        }

        if ( System.getenv("CUSTOMER_S3_FOLDER") == null ){
            System.out.println("CUSTOMER_S3_FOLDER not defined. Using default value "+folder);
        }



        Class clazz = Customer.class;
        InputStream inputStream = clazz.getResourceAsStream("/customers.json");
        Region region = Region.US_EAST_1;
        s3 = S3Client.builder().region(region).build();

        String key = folder + filename;
        System.out.println("Retrieving data from Source and loading into S3");

        // Put Object
        s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key)
                        .build(),
                RequestBody.fromByteBuffer(getRandomByteBuffer(10_000)));

        System.out.println("Completed Loading Data into S3. Exiting");
        System.exit(0);


    }

    /**
     * Uploading an object to S3 in parts
     */
    private static void multipartUpload(S3Client s3,String bucketName, String key) throws IOException {

        int MB = 1024 * 1024;
        // snippet-start:[s3.java2.s3_object_operations.upload_multi_part]
        // First create a multipart upload and get upload id
        CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucketName).key(key)
                .build();
        CreateMultipartUploadResponse response = s3.createMultipartUpload(createMultipartUploadRequest);
        String uploadId = response.uploadId();
        System.out.println(uploadId);

        // Upload all the different parts of the object
        UploadPartRequest uploadPartRequest1 = UploadPartRequest.builder().bucket(bucketName).key(key)
                .uploadId(uploadId)
                .partNumber(1).build();
        String etag1 = s3.uploadPart(uploadPartRequest1, RequestBody.fromByteBuffer(getRandomByteBuffer(5 * MB))).eTag();
        CompletedPart part1 = CompletedPart.builder().partNumber(1).eTag(etag1).build();

        UploadPartRequest uploadPartRequest2 = UploadPartRequest.builder().bucket(bucketName).key(key)
                .uploadId(uploadId)
                .partNumber(2).build();
        String etag2 = s3.uploadPart(uploadPartRequest2, RequestBody.fromByteBuffer(getRandomByteBuffer(3 * MB))).eTag();
        CompletedPart part2 = CompletedPart.builder().partNumber(2).eTag(etag2).build();


        // Finally call completeMultipartUpload operation to tell S3 to merge all uploaded
        // parts and finish the multipart operation.
        CompletedMultipartUpload completedMultipartUpload = CompletedMultipartUpload.builder().parts(part1, part2).build();
        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                CompleteMultipartUploadRequest.builder().bucket(bucketName).key(key).uploadId(uploadId)
                        .multipartUpload(completedMultipartUpload).build();
        s3.completeMultipartUpload(completeMultipartUploadRequest);
        // snippet-end:[s3.java2.s3_object_operations.upload_multi_part]
    }

    private static ByteBuffer getRandomByteBuffer(int size) throws IOException {
        byte[] b = new byte[size];
        new Random().nextBytes(b);
        return ByteBuffer.wrap(b);
    }
}
