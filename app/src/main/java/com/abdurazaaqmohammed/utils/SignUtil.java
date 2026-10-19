package com.abdurazaaqmohammed.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Build;

import com.abdurazaaqmohammed.AntiSplit.R;
import com.abdurazaaqmohammed.AntiSplit.main.MainActivity;
import com.aefyr.pseudoapksigner.IOUtils;
import com.aefyr.pseudoapksigner.PseudoApkSigner;
import com.android.apksig.ApkSigner;
import com.android.apksig.apk.ApkFormatException;
import com.starry.FileUtils;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    public static void signApk(InputStream key, String password, File inputApk, File output) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException, ApkFormatException, SignatureException, InvalidKeyException, UnrecoverableEntryException {
        signApk(key, password, inputApk, output, true, true, true);
    }

    public static void signApk(InputStream key, String password, File inputApk, File output, boolean v1, boolean v2, boolean v3) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException, ApkFormatException, SignatureException, InvalidKeyException, UnrecoverableEntryException {
        char[] pw = password.toCharArray();

        KeyStore keystore = KeyStore.getInstance("BKS");
        keystore.load(key, pw);

        String alias = keystore.aliases().nextElement();

        new ApkSigner.Builder(Collections.singletonList(new ApkSigner.SignerConfig.Builder("CERT",
                ((KeyStore.PrivateKeyEntry) keystore.getEntry(alias, new KeyStore.PasswordProtection(pw))).getPrivateKey(),
                Collections.singletonList((X509Certificate) keystore.getCertificate(alias))).build()))
                .setInputApk(inputApk)
                .setOutputApk(output)
                .setCreatedBy("Android Gradle 8.0.2")
                .setV1SigningEnabled(v1)
                .setV2SigningEnabled(v2)
                .setV3SigningEnabled(v3).build().sign();
    }

    public static void signDebugKey(Context c, File inputApk, File output, boolean v1, boolean v2, boolean v3) throws IOException, ApkFormatException, UnrecoverableEntryException, CertificateException, KeyStoreException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        try (InputStream key = c.getAssets().open("debug23.keystore")) {
            signApk(key, "android", inputApk, output, v1, v2, v3);
        }
    }

    public static void signDebugKey(Context c, File inputApk, File output) throws IOException, ApkFormatException, UnrecoverableEntryException, CertificateException, KeyStoreException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        signDebugKey(c, inputApk, output, true, true, true);
    }

    public static void signPseudoApkSigner(File temp, MainActivity context, Uri out, Exception e) throws Exception {
        String msg = context.getRss().getString(R.string.sign_failed);
        if(Build.VERSION.SDK_INT < 30) {
            // When I tried signing with apksig in AVD with sdk 10 java.security is throwing some error saying something not found
            // Apparently 11 is the last version that supports v1 signing alone.
            try (InputStream is = com.abdurazaaqmohammed.utils.FileUtils.getInputStream(temp)) {
                final String FILE_NAME_PAST = "testkey.past";
                final String FILE_NAME_PRIVATE_KEY = "testkey.pk8";
                File signingEnvironment = new File(context.getFilesDir(), "signing");
                File pastFile = new File(signingEnvironment, FILE_NAME_PAST);
                File privateKeyFile = new File(signingEnvironment, FILE_NAME_PRIVATE_KEY);

                if (!pastFile.exists() || !privateKeyFile.exists()) {
                    signingEnvironment.mkdir();
                    IOUtils.copyFileFromAssets(context, FILE_NAME_PAST, pastFile);
                    IOUtils.copyFileFromAssets(context, FILE_NAME_PRIVATE_KEY, privateKeyFile);
                }

                try(OutputStream os = FileUtils.getOutputStream(out, context)) {
                    PseudoApkSigner.sign(is, os, pastFile, privateKeyFile);
                }
            } catch (Exception e2) {
                context.getLogger().logMessage(msg);
                try(OutputStream os = FileUtils.getOutputStream(out, context)) {
                    com.abdurazaaqmohammed.utils.FileUtils.copyFile(temp, os);
                }
                StringBuilder errs = new StringBuilder(e2.toString());
                for(StackTraceElement stackTraceElement : e2.getStackTrace()) errs.append('\n').append(stackTraceElement);
                for(StackTraceElement stackTraceElement : e.getStackTrace()) errs.append('\n').append(stackTraceElement);
                throw(new RuntimeException(errs.toString())); // for showError
            }
        } else {
            context.getLogger().logMessage(msg);
            try(OutputStream os = FileUtils.getOutputStream(out, context)) {
                com.abdurazaaqmohammed.utils.FileUtils.copyFile(temp, os);
            }
            throw((e)); // for showError
        }
    }
}