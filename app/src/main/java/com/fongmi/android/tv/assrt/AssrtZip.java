package com.fongmi.android.tv.assrt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class AssrtZip {

    private AssrtZip() {
    }

    public static List<String> listText(File zip) throws Invalid, IOException {
        List<String> names = new ArrayList<>();
        try (ZipFile archive = new ZipFile(zip)) {
            Enumeration<? extends ZipEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (AssrtRules.unsafeZipName(entry.getName())) throw new Invalid();
                if (!entry.isDirectory() && AssrtRules.textSubtitle(entry.getName())) names.add(entry.getName());
            }
        } catch (Invalid e) {
            throw e;
        } catch (IOException e) {
            throw new Invalid();
        }
        return names;
    }

    public static File extract(File zip, String entryName, File root) throws Invalid, IOException {
        try (ZipFile archive = new ZipFile(zip)) {
            Enumeration<? extends ZipEntry> entries = archive.entries();
            while (entries.hasMoreElements()) {
                if (AssrtRules.unsafeZipName(entries.nextElement().getName())) throw new Invalid();
            }
            ZipEntry entry = archive.getEntry(entryName);
            if (entry == null || entry.isDirectory() || !AssrtRules.textSubtitle(entryName)) throw new Invalid();
            File dest = resolve(root, entryName);
            File parent = dest.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.exists()) throw new IOException("dir");
            try (InputStream input = archive.getInputStream(entry); FileOutputStream output = new FileOutputStream(dest)) {
                byte[] buffer = new byte[16384];
                int read;
                while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            }
            return dest;
        } catch (Invalid e) {
            throw e;
        } catch (java.util.zip.ZipException e) {
            throw new Invalid();
        }
    }

    private static File resolve(File root, String entryName) throws Invalid, IOException {
        File dest = new File(root, entryName.replace('\\', '/'));
        String rootPath = root.getCanonicalPath();
        String destPath = dest.getCanonicalPath();
        if (!destPath.equals(rootPath) && !destPath.startsWith(rootPath + File.separator)) throw new Invalid();
        return dest;
    }

    public static final class Invalid extends Exception {
    }
}
