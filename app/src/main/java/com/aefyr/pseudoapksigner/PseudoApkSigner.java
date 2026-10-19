package com.aefyr.pseudoapksigner;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class PseudoApkSigner {
    public static void sign(InputStream apkInputStream, OutputStream output, File mTemplateFile, File privateKey) throws Exception {
        final RSAPrivateKey mPrivateKey = Utils.readPrivateKey(privateKey);
        final String HASHING_ALGORITHM = "SHA1";
        final String[] META_INF_FILES_TO_SKIP_ENDINGS = new String[]{"manifest.mf", ".sf", ".rsa", ".dsa", ".ec"};

        ManifestBuilder manifest = new ManifestBuilder();
        SignatureFileGenerator signature = new SignatureFileGenerator(manifest, HASHING_ALGORITHM);

        ZipInputStream apkZipInputStream = new ZipInputStream(apkInputStream);

        ZipAlignZipOutputStream zipOutputStream = ZipAlignZipOutputStream.create(output, 4);
        MessageDigest messageDigest = MessageDigest.getInstance(HASHING_ALGORITHM);
        ZipEntry zipEntry;
        OUTER:
        while ((zipEntry = apkZipInputStream.getNextEntry()) != null) {
            if (zipEntry.isDirectory())
                continue;

            if(zipEntry.getName().toLowerCase(java.util.Locale.ROOT).startsWith("meta-inf/")) {
                for(String fileToSkipEnding: META_INF_FILES_TO_SKIP_ENDINGS) {
                    if(zipEntry.getName().toLowerCase(java.util.Locale.ROOT).endsWith(fileToSkipEnding))
                        continue OUTER;
                }
            }


            messageDigest.reset();
            DigestInputStream entryInputStream = new DigestInputStream(apkZipInputStream, messageDigest);

            ZipEntry newZipEntry = new ZipEntry(zipEntry.getName());
            newZipEntry.setMethod(zipEntry.getMethod());
            if (zipEntry.getMethod() == ZipEntry.STORED) {
                newZipEntry.setSize(zipEntry.getSize());
                newZipEntry.setCompressedSize(zipEntry.getSize());
                newZipEntry.setCrc(zipEntry.getCrc());
            }

            zipOutputStream.setAlignment(newZipEntry.getName().endsWith(".so") ? 4096 : 4);
            zipOutputStream.putNextEntry(newZipEntry);
            Utils.copyStream(entryInputStream, zipOutputStream);
            zipOutputStream.closeEntry();
            apkZipInputStream.closeEntry();

            ManifestBuilder.ManifestEntry manifestEntry = new ManifestBuilder.ManifestEntry();
            manifestEntry.setAttribute("Name", zipEntry.getName());
            manifestEntry.setAttribute(HASHING_ALGORITHM + "-Digest", Utils.base64Encode(messageDigest.digest()));
            manifest.addEntry(manifestEntry);
        }

        zipOutputStream.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
        zipOutputStream.write(manifest.build().getBytes(Constants.UTF8));
        zipOutputStream.closeEntry();

        String mSignerName = "CERT";
        zipOutputStream.putNextEntry(new ZipEntry(String.format("META-INF/%s.SF", mSignerName)));
        zipOutputStream.write(signature.generate().getBytes(Constants.UTF8));
        zipOutputStream.closeEntry();

        zipOutputStream.putNextEntry(new ZipEntry(String.format("META-INF/%s.RSA", mSignerName)));
        zipOutputStream.write(Utils.readFile(mTemplateFile));
        zipOutputStream.write(Utils.sign(HASHING_ALGORITHM, mPrivateKey, signature.generate().getBytes(Constants.UTF8)));
        zipOutputStream.closeEntry();

        apkZipInputStream.close();
        zipOutputStream.close();
    }
}
