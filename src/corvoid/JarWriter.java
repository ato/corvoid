package corvoid;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

class JarWriter implements Closeable {
    private final byte[] buffer = new byte[65536];
    private final Set<String> seen = new HashSet<>();
    private final ZipOutputStream out;

    public JarWriter(OutputStream out) {
        this.out = new ZipOutputStream(out);
    }

    private void copyStream(InputStream in, OutputStream out) throws IOException {
        for (;;) {
            int n = in.read(buffer);
            if (n < 0) break;
            out.write(buffer, 0, n);
        }
    }

    public void putJarContents(ZipFile in) throws IOException {
        Enumeration<? extends ZipEntry> e = in.entries();
        while (e.hasMoreElements()) {
            ZipEntry entry = e.nextElement();
            if (!seen.add(entry.getName())) {
                // TODO: concatenate duplicate META-INF/services/*
                continue;
            }
            entry.setCompressedSize(-1);
            out.putNextEntry(entry);
            try (InputStream stream = in.getInputStream(entry)) {
                copyStream(stream, out);
            }
            out.closeEntry();
        }
    }

    public void put(File file) throws IOException {
        put(file, "");
    }

    public void putDirContents(File dir) throws IOException {
        putDirContents(dir, "");
    }

    private void putDirContents(File dir, String prefix) throws IOException {
        for (File file : requireNonNull(dir.listFiles())) {
            put(file, prefix);
        }
    }

    private void put(File file, String prefix) throws IOException {
        if (file.isDirectory()) {
            putDirContents(file, prefix + file.getName() + "/");
        } else if (file.isFile()) {
            String path = prefix + file.getName();
            if (!seen.add(path)) {
                return;
            }
            out.putNextEntry(new ZipEntry(path));
            try (FileInputStream in = new FileInputStream(file)) {
                copyStream(in, out);
            }
            out.closeEntry();
        }
    }

    public void writeManifest(String mainClass) throws IOException {
        String name = "META-INF/MANIFEST.MF";
        String content = "Manifest-Version: 1.0\n";
        if (mainClass != null) content += "Main-Class: " + mainClass + "\n";
        seen.add(name);
        out.putNextEntry(new ZipEntry(name));
        out.write(content.getBytes(UTF_8));
        out.closeEntry();
    }

    @Override
    public void close() throws IOException {
        this.out.close();
    }
}
