/*
 * acme4j - Java ACME client
 *
 * Copyright (C) 2015 Richard "Shred" Körber
 *   http://acme4j.shredzone.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package org.shredzone.acme4j.util;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;
import java.util.TreeMap;

import org.jose4j.base64url.Base64Url;
import org.jose4j.json.JsonUtil;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKey.OutputControlLevel;
import org.jose4j.lang.JoseException;

/**
 * Some utility methods for unit tests.
 *
 * @author Richard "Shred" Körber
 */
public final class TestUtils {
    public static final String N = "pZsTKY41y_CwgJ0VX7BmmGs_7UprmXQMGPcnSbBeJAjZHA9SyyJKaWv4fNUdBIAX3Y2QoZixj50nQLyLv2ng3pvEoRL0sx9ZHgp5ndAjpIiVQ_8V01TTYCEDUc9ii7bjVkgFAb4ValZGFJZ54PcCnAHvXi5g0ELORzGcTuRqHVAUckMV2otr0g0u_5bWMm6EMAbBrGQCgUGjbZQHjava1Y-5tHXZkPBahJ2LvKRqMmJUlr0anKuJJtJUG03DJYAxABv8YAaXFBnGw6kKJRpUFAC55ry4sp4kGy0NrK2TVWmZW9kStniRv4RaJGI9aZGYwQy2kUykibBNmWEQUlIwIw";
    public static final String E = "AQAB";
    public static final String KTY = "RSA";
    public static final String THUMBPRINT = "HnWjTDnyqlCrm6tZ-6wX-TrEXgRdeNu9G71gqxSO6o0";

    private TestUtils() {
        // utility class without constructor
    }

    /**
     * Reads a resource as byte array.
     *
     * @param name
     *            Resource name
     * @return Resource content as byte array.
     */
    public static byte[] getResourceAsByteArray(String name) throws IOException {
        byte[] buffer = new byte[2048];
        try (InputStream in = TestUtils.class.getResourceAsStream(name);
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            int len;
            while ((len = in.read(buffer)) >= 0) {
                out.write(buffer, 0, len);
            }
            return out.toByteArray();
        }
    }

    /**
     * Reads a resource as String.
     *
     * @param name
     *            Resource name. The content is expected to be utf-8 encoded.
     * @return Resource contents as string
     */
    public static String getResourceAsString(String name) throws IOException {
        try (InputStreamReader in = new InputStreamReader(
                        TestUtils.class.getResourceAsStream(name), "utf-8");
                StringWriter out = new StringWriter()) {
            int ch;
            while ((ch = in.read()) >= 0) {
                out.write(ch);
            }
            return out.toString();
        }
    }

    /**
     * Reads a JSON resource and parses it.
     *
     * @param name
     *            Resource name of a utf-8 encoded JSON file.
     * @return Parsed contents
     */
    public static Map<String, Object> getResourceAsJsonMap(String name) throws IOException {
        try {
            return JsonUtil.parseJson(getResourceAsString(name));
        } catch (JoseException ex) {
            throw new IOException("JSON error", ex);
        }
    }

    /**
     * Creates a standard key pair for testing. This keypair is read from a test resource
     * and is guaranteed not to change between test runs.
     * <p>
     * The constants {@link #N}, {@link #E}, {@link #KTY} and {@link #THUMBPRINT} are
     * related to the returned key pair and can be used for asserting results.
     *
     * @return {@link KeyPair} for testing
     */
    public static KeyPair createKeyPair() throws IOException {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(KTY);

            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(
                    getResourceAsByteArray("/public.key"));
            PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(
                    getResourceAsByteArray("/private.key"));
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

            return new KeyPair(publicKey, privateKey);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Creates a standard certificate for testing. This certificate is read from a test
     * resource and is guaranteed not to change between test runs.
     *
     * @return {@link X509Certificate} for testing
     */
    public static X509Certificate createCertificate() throws IOException {
        try (InputStream cert = TestUtils.class.getResourceAsStream("/cert.pem")) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) certificateFactory.generateCertificate(cert);
        } catch (CertificateException ex) {
            throw new IOException(ex);
        }
    }

    /**
     * Generates a new keypair for unit tests, and return its N, E, KTY and THUMBPRINT
     * parameters to be set in the {@link TestUtils} class.
     */
    public static void main(String... args) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        try (FileOutputStream out = new FileOutputStream("public.key")) {
            out.write(keyPair.getPublic().getEncoded());
        }

        try (FileOutputStream out = new FileOutputStream("private.key")) {
            out.write(keyPair.getPrivate().getEncoded());
        }

        final JsonWebKey jwk = JsonWebKey.Factory.newJwk(keyPair.getPublic());
        Map<String, Object> params = new TreeMap<>(jwk.toParams(OutputControlLevel.PUBLIC_ONLY));
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(JsonUtil.toJson(params).getBytes("UTF-8"));
        byte[] thumbprint = md.digest();

        System.out.println("N = " + params.get("n"));
        System.out.println("E = " + params.get("e"));
        System.out.println("KTY = " + params.get("kty"));
        System.out.println("THUMBPRINT = " + Base64Url.encode(thumbprint));
    }

}
