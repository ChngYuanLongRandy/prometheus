package com.prometheus.watcher.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/** Upload, pre-sign, and delete MP3 objects in the private S3 audio bucket. */
@Service
public class S3AudioService {

    private final S3Client s3;
    private final S3Presigner presigner;

    @Value("${aws.s3.audio-bucket}")
    private String bucket;

    @Value("${aws.audio.presigned-url-expiry-hours:1}")
    private int expiryHours;

    public S3AudioService(S3Client s3, S3Presigner presigner) {
        this.s3 = s3;
        this.presigner = presigner;
    }

    public void upload(String key, byte[] mp3) {
        s3.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType("audio/mpeg")
                        .contentLength((long) mp3.length)
                        .build(),
                RequestBody.fromBytes(mp3));
    }

    /** Returns a time-limited pre-signed GET URL (default 1 hour). */
    public String presignedUrl(String key) {
        return presigner.presignGetObject(r -> r
                        .signatureDuration(Duration.ofHours(expiryHours))
                        .getObjectRequest(g -> g.bucket(bucket).key(key)))
                .url().toString();
    }

    public void delete(String key) {
        s3.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }
}
