package com.abdurazaaqmohammed.utils;

import android.content.Context;

import com.android.apksig.ApkSigner;
import com.android.apksig.apk.ApkFormatException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;

public class SignUtil {

    private static final String DEBUG_KEYSTORE = "debug23.keystore";
    private static final String DEBUG_PASSWORD = "android";

    public static void signApk(InputStream key, String password, File inputApk, File output)
            throws KeyStoreException, CertificateException, IOException,
            NoSuchAlgorithmException, ApkFormatException, SignatureException,
            InvalidKeyException, UnrecoverableEntryException {
        signApk(key, password, inputApk, output, true, true, true);
    }

    public static void signApk(InputStream key, String password, File inputApk, File output,
            boolean v1, boolean v2, boolean v3)
            throws KeyStoreException, CertificateException, IOException,
            NoSuchAlgorithmException, ApkFormatException, SignatureException,
            InvalidKeyException, UnrecoverableEntryException {
        char[] passwordChars = password.toCharArray();

        KeyStore keystore = KeyStore.getInstance("BKS");
        keystore.load(key, passwordChars);

        String alias = keystore.aliases().nextElement();
        KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) keystore.getEntry(alias,
                new KeyStore.PasswordProtection(passwordChars));

        new ApkSigner.Builder(Collections.singletonList(new ApkSigner.SignerConfig.Builder(
                "CERT",
                entry.getPrivateKey(),
                Collections.singletonList((X509Certificate) keystore.getCertificate(alias)))
                .build()))
                .setInputApk(inputApk)
                .setOutputApk(output)
                .setCreatedBy("Android Gradle 8.0.2")
                .setV1SigningEnabled(v1)
                .setV2SigningEnabled(v2)
                .setV3SigningEnabled(v3)
                .build()
                .sign();
    }

    public static void signDebugKey(Context context, File inputApk, File output)
            throws IOException, ApkFormatException, UnrecoverableEntryException,
            CertificateException, KeyStoreException, NoSuchAlgorithmException,
            SignatureException, InvalidKeyException {
        try (InputStream key = context.getAssets().open(DEBUG_KEYSTORE)) {
            signApk(key, DEBUG_PASSWORD, inputApk, output);
        }
    }
}
