/*
 *  Copyright (C) 2022 github.com/REAndroid
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.reandroid;

import android.app.Activity;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;

import com.abdurazaaqmohammed.AntiSplit.R;
import com.abdurazaaqmohammed.AntiSplit.main.MyAPKLogger;
import com.abdurazaaqmohammed.utils.DeviceSpecsUtil;
import com.abdurazaaqmohammed.utils.FileUtils;
import com.abdurazaaqmohammed.utils.SignUtil;
import com.abdurazaaqmohammed.utils.UriNames;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.reandroid.apk.ApkBundle;
import com.reandroid.apk.ApkModule;
import com.reandroid.apkeditor.common.AndroidManifestHelper;
import com.reandroid.app.AndroidManifest;
import com.reandroid.archive.ArchiveFile;
import com.reandroid.archive.InputSource;
import com.reandroid.archive.ZipEntryMap;
import com.reandroid.arsc.chunk.TableBlock;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.arsc.chunk.xml.ResXmlAttribute;
import com.reandroid.arsc.chunk.xml.ResXmlElement;
import com.reandroid.arsc.container.SpecTypePair;
import com.reandroid.arsc.model.ResourceEntry;
import com.reandroid.arsc.value.Entry;
import com.reandroid.arsc.value.ResValue;
import com.reandroid.arsc.value.ValueType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Merger {

    private static final long PAIRIP_DIALOG_TIMEOUT_MINUTES = 10;
    private static final String PAIRIP_LIBRARY = "/libpairipcore.so";

    private final File workingDirectory;
    private final Activity activity;
    private final Resources resources;
    private final MyAPKLogger logger;
    private final int compressionLevel;

    private Uri signedApkUri;

    public Merger(File workingDirectory, Activity activity, Resources resources,
            MyAPKLogger logger, int compressionLevel) {
        this.workingDirectory = workingDirectory;
        this.activity = activity;
        this.resources = resources;
        this.logger = logger;
        this.compressionLevel = compressionLevel;
    }

    public Uri getSignedApkUri() {
        return signedApkUri;
    }

    public File run(Uri splitApkUri, List<String> splitsToNotInclude, boolean signApk,
            boolean force) throws Exception {
        logger.logMessage(R.string.searching);
        try (ApkBundle bundle = new ApkBundle(compressionLevel)) {
            if (splitApkUri == null) {
                bundle.loadApkDirectory(workingDirectory);
            } else {
                logger.logMessage("MIME Type " + activity.getContentResolver().getType(splitApkUri));
                loadArchive(splitApkUri, splitsToNotInclude, bundle);
            }
            return run(bundle, signApk, force);
        }
    }

    public File run(ApkBundle bundle, boolean signApk, boolean force) throws Exception {
        logger.logMessage("Found modules: " + bundle.getApkModuleList().size());

        signedApkUri = null;
        boolean sign = signApk;
        if (containsPairipIntegrity(bundle)) {
            if (!confirmPairipWarning())
                return null;
            sign = false;
        }

        File mergedApk = mergeAndSanitize(bundle, force);
        return sign ? sign(mergedApk) : mergedApk;
    }

    private void loadArchive(Uri splitApkUri, List<String> splitsToNotInclude, ApkBundle bundle)
            throws IOException {
        boolean checkSplits = splitsToNotInclude != null && !splitsToNotInclude.isEmpty();

        File archiveFile = DeviceSpecsUtil.getCachedArchive(splitApkUri);
        boolean deleteAfterUse = false;
        if (archiveFile == null) {
            archiveFile = copyArchive(splitApkUri);
            deleteAfterUse = true;
        }

        try {
            extractApks(archiveFile, checkSplits, splitsToNotInclude);
            bundle.loadApkDirectory(workingDirectory);
        } catch (FileNotFoundException e) {
            throw new IOException(e.getMessage() + " file " + splitApkUri + ' '
                    + UriNames.displayName(activity, splitApkUri), e);
        } finally {
            if (deleteAfterUse)
                archiveFile.delete();
        }
    }

    private File copyArchive(Uri uri) throws IOException {
        File copy = new File(workingDirectory, "input_" + System.currentTimeMillis() + ".zip");
        try (InputStream input = activity.getContentResolver().openInputStream(uri)) {
            if (input == null)
                throw new IOException("Unable to open " + uri);
            FileUtils.copyFile(input, copy);
        }
        return copy;
    }

    private void extractApks(File archiveFile, boolean checkSplits, List<String> excludedSplits)
            throws IOException {
        try (ArchiveFile archive = new ArchiveFile(archiveFile)) {
            for (InputSource entry : archive.getInputSources()) {
                String name = entry.getName();
                if (!name.endsWith(".apk")) {
                    logger.logMessage(resources.getString(R.string.skipping) + name
                            + resources.getString(R.string.not_apk));
                    continue;
                }
                if (checkSplits && excludedSplits.contains(name)) {
                    logger.logMessage(resources.getString(R.string.skipping) + name
                            + resources.getString(R.string.unselected));
                    continue;
                }
                File target = resolveTarget(name);
                if (target == null) {
                    logger.logMessage("Skipped invalid path: " + name);
                    continue;
                }
                try (InputStream input = entry.openStream()) {
                    FileUtils.copyFile(input, target);
                }
            }
        }
    }

    private File resolveTarget(String entryName) throws IOException {
        File target = new File(workingDirectory, entryName);
        String basePath = workingDirectory.getCanonicalPath() + File.separator;
        if (!target.getCanonicalPath().startsWith(basePath))
            return null;
        return target;
    }

    private File mergeAndSanitize(ApkBundle bundle, boolean force) throws Exception {
        try (ApkModule mergedModule = bundle.mergeModules(!force)) {
            if (mergedModule.hasAndroidManifest())
                sanitizeManifest(mergedModule);

            logger.logMessage(R.string.saving);
            File mergedApk = new File(workingDirectory,
                    "merged_" + System.currentTimeMillis() + ".apk");
            mergedModule.writeApk(mergedApk);
            return mergedApk;
        }
    }

    private File sign(File unsignedApk) {
        logger.logMessage(R.string.signing);
        File signedApk = new File(workingDirectory,
                "signed_" + System.currentTimeMillis() + ".apk");
        try {
            SignUtil.signDebugKey(activity, unsignedApk, signedApk);
            signedApkUri = FileProvider.getUriForFile(activity,
                    activity.getPackageName() + ".fileprovider", signedApk);
            if (!unsignedApk.delete())
                logger.logMessage("Unable to delete unsigned APK");
            return signedApk;
        } catch (Exception e) {
            logger.logMessage(R.string.sign_failed);
            logger.logMessage(e.toString());
            signedApk.delete();
            return unsignedApk;
        }
    }

    private boolean containsPairipIntegrity(ApkBundle bundle) {
        for (ApkModule module : bundle.getApkModuleList()) {
            for (String abi : supportedAbis()) {
                if (module.containsFile("lib/" + abi + PAIRIP_LIBRARY))
                    return true;
            }
        }
        return false;
    }

    private boolean confirmPairipWarning() throws InterruptedException {
        if (activity.isFinishing() || activity.isDestroyed())
            return false;

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] proceed = {false};
        activity.runOnUiThread(() -> {
            try {
                new MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.warning)
                        .setMessage(R.string.pairip_warning)
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            proceed[0] = true;
                            latch.countDown();
                        })
                        .setNegativeButton(R.string.cancel, (dialog, which) -> latch.countDown())
                        .show();
            } catch (Exception e) {
                latch.countDown();
            }
        });

        if (!latch.await(PAIRIP_DIALOG_TIMEOUT_MINUTES, TimeUnit.MINUTES))
            return false;
        return proceed[0];
    }

    private void sanitizeManifest(ApkModule mergedModule) {
        AndroidManifestBlock manifest = mergedModule.getAndroidManifest();
        logger.logMessage(R.string.sanitizing_manifest);

        AndroidManifestHelper.removeAttributeFromManifestById(manifest,
                AndroidManifest.ID_requiredSplitTypes, logger);
        AndroidManifestHelper.removeAttributeFromManifestById(manifest,
                AndroidManifest.ID_splitTypes, logger);
        AndroidManifestHelper.removeAttributeFromManifestByName(manifest,
                AndroidManifest.NAME_splitTypes, logger);
        AndroidManifestHelper.removeAttributeFromManifestByName(manifest,
                AndroidManifest.NAME_requiredSplitTypes, logger);
        AndroidManifestHelper.removeAttributeFromManifestByName(manifest,
                AndroidManifest.NAME_splitTypes, logger);
        AndroidManifestHelper.removeAttributeFromManifestAndApplication(manifest,
                AndroidManifest.ID_extractNativeLibs, logger,
                AndroidManifest.NAME_extractNativeLibs);
        AndroidManifestHelper.removeAttributeFromManifestAndApplication(manifest,
                AndroidManifest.ID_isSplitRequired, logger,
                AndroidManifest.NAME_isSplitRequired);

        removeSplitMetadata(mergedModule, manifest);
        manifest.refresh();
    }

    private void removeSplitMetadata(ApkModule mergedModule, AndroidManifestBlock manifest) {
        ResXmlElement application = manifest.getApplicationElement();
        if (application == null)
            return;

        boolean splitsRemoved = false;
        for (ResXmlElement meta : AndroidManifestHelper.listSplitRequired(application)) {
            if (!splitsRemoved)
                splitsRemoved = removeSplitMetadataEntry(mergedModule, meta);
            logger.logMessage("Removed-element : <" + meta.getName() + "> name=\""
                    + AndroidManifestBlock.getAndroidNameValue(meta) + "\"");
            application.remove(meta);
        }
    }

    private boolean removeSplitMetadataEntry(ApkModule mergedModule, ResXmlElement meta) {
        ResXmlAttribute nameAttribute = meta.searchAttributeByResourceId(AndroidManifest.ID_name);
        if (nameAttribute == null
                || !"com.android.vending.splits".equals(nameAttribute.getValueAsString()))
            return false;

        ResXmlAttribute valueAttribute =
                meta.searchAttributeByResourceId(AndroidManifest.ID_value);
        if (valueAttribute == null)
            valueAttribute = meta.searchAttributeByResourceId(AndroidManifest.ID_resource);
        if (valueAttribute == null || valueAttribute.getValueType() != ValueType.REFERENCE
                || !mergedModule.hasTableBlock())
            return false;

        TableBlock tableBlock = mergedModule.getTableBlock();
        ResourceEntry resourceEntry = tableBlock.getResource(valueAttribute.getData());
        if (resourceEntry == null)
            return false;

        ZipEntryMap zipEntryMap = mergedModule.getZipEntryMap();
        for (Entry entry : resourceEntry) {
            if (entry == null)
                continue;
            ResValue resValue = entry.getResValue();
            if (resValue == null)
                continue;
            String path = resValue.getValueAsString();
            if (path != null) {
                logger.logMessage(resources.getString(R.string.removed_table_entry) + " " + path);
                zipEntryMap.remove(path);
            }
            entry.setNull(true);
            if (entry.getTypeBlock() != null) {
                SpecTypePair specTypePair = entry.getTypeBlock().getParentSpecTypePair();
                if (specTypePair != null)
                    specTypePair.removeNullEntries(entry.getId());
            }
        }
        return true;
    }

    private static String[] supportedAbis() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            return Build.SUPPORTED_ABIS;
        return new String[]{Build.CPU_ABI, Build.CPU_ABI2};
    }
}
