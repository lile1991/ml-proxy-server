package io.ml.proxy.utils.io;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class FileUtils {
    public static String readFileToString(File file, Charset charsetName) throws IOException {
        try (InputStream in = openInputStream(file)) {
            return IOUtils.toString(in, charsetName);
        }
    }
    public static List<String> readLines(File file, Charset charsetName) throws IOException {
        try (InputStream in = openInputStream(file)) {
            return IOUtils.readLines(in, charsetName);
        }
    }

    public static FileInputStream openInputStream(File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            } else if (!file.canRead()) {
                throw new IOException("File '" + file + "' cannot be read");
            } else {
                return new FileInputStream(file);
            }
        } else {
            throw new FileNotFoundException("File '" + file + "' does not exist");
        }
    }

    public static boolean deleteQuietly(File file) {
        if (file == null) {
            return false;
        } else {
            if(file.isDirectory()) {
                File[] files = file.listFiles();
                if(files != null) {
                    for (File f : files) {
                        if (f.isDirectory()) {
                            deleteQuietly(f);
                        } else {
                            boolean isDeleted = f.delete();
                        }
                    }
                }
                boolean isDeleted = file.delete();
                return isDeleted;
            } else {
                boolean isDeleted = file.delete();
                return isDeleted;
            }
        }
    }


    public static void copyDirectory(File srcDir, File destDir, FileFilter filter, boolean preserveFileDate) throws IOException {
        checkFileRequirements(srcDir, destDir);
        if (!srcDir.isDirectory()) {
            throw new IOException("Source '" + srcDir + "' exists but is not a directory");
        }
        if (srcDir.getCanonicalPath().equals(destDir.getCanonicalPath())) {
            throw new IOException("Source '" + srcDir + "' and destination '" + destDir + "' are the same");
        }

        // Cater for destination being directory within the source directory (see IO-141)
        List<String> exclusionList = null;
        if (destDir.getCanonicalPath().startsWith(srcDir.getCanonicalPath())) {
            final File[] srcFiles = filter == null ? srcDir.listFiles() : srcDir.listFiles(filter);
            if (srcFiles != null && srcFiles.length > 0) {
                exclusionList = new ArrayList<>(srcFiles.length);
                for (final File srcFile : srcFiles) {
                    final File copiedFile = new File(destDir, srcFile.getName());
                    exclusionList.add(copiedFile.getCanonicalPath());
                }
            }
        }
        doCopyDirectory(srcDir, destDir, filter, preserveFileDate, exclusionList);
    }

    /**
     * Checks requirements for file copy.
     *
     * @param source the source file
     * @param destination the destination
     * @throws FileNotFoundException if the destination does not exist
     */
    private static void checkFileRequirements(final File source, final File destination) throws FileNotFoundException {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "target");
        if (!source.exists()) {
            throw new FileNotFoundException("Source '" + source + "' does not exist");
        }
    }


    /**
     * Internal copy directory method.
     *
     * @param srcDir           the validated source directory, must not be {@code null}
     * @param destDir          the validated destination directory, must not be {@code null}
     * @param filter           the filter to apply, null means copy all directories and files
     * @param preserveFileDate whether to preserve the file date
     * @param exclusionList    List of files and directories to exclude from the copy, may be null
     * @throws IOException if an error occurs
     * @since 1.1
     */
    private static void doCopyDirectory(final File srcDir, final File destDir, final FileFilter filter,
                                        final boolean preserveFileDate, final List<String> exclusionList)
            throws IOException {
        // recurse
        final File[] srcFiles = filter == null ? srcDir.listFiles() : srcDir.listFiles(filter);
        if (srcFiles == null) {  // null if abstract pathname does not denote a directory, or if an I/O error occurs
            throw new IOException("Failed to list contents of " + srcDir);
        }
        if (destDir.exists()) {
            if (destDir.isDirectory() == false) {
                throw new IOException("Destination '" + destDir + "' exists but is not a directory");
            }
        } else {
            if (!destDir.mkdirs() && !destDir.isDirectory()) {
                throw new IOException("Destination '" + destDir + "' directory cannot be created");
            }
        }
        if (destDir.canWrite() == false) {
            throw new IOException("Destination '" + destDir + "' cannot be written to");
        }
        for (final File srcFile : srcFiles) {
            final File dstFile = new File(destDir, srcFile.getName());
            if (exclusionList == null || !exclusionList.contains(srcFile.getCanonicalPath())) {
                if (srcFile.isDirectory()) {
                    doCopyDirectory(srcFile, dstFile, filter, preserveFileDate, exclusionList);
                } else {
                    doCopyFile(srcFile, dstFile, preserveFileDate);
                }
            }
        }

        // Do this last, as the above has probably affected directory metadata
        if (preserveFileDate) {
            destDir.setLastModified(srcDir.lastModified());
        }
    }

    private static void doCopyFile(final File srcFile, final File destFile, final boolean preserveFileDate)
            throws IOException {
        if (destFile.exists() && destFile.isDirectory()) {
            throw new IOException("Destination '" + destFile + "' exists but is a directory");
        }

        final Path srcPath = srcFile.toPath();
        final Path destPath = destFile.toPath();
        final long newLastModifed = preserveFileDate ? srcFile.lastModified() : destFile.lastModified();
        Files.copy(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING);

        // TODO IO-386: Do we still need this check?
        checkEqualSizes(srcFile, destFile, Files.size(srcPath), Files.size(destPath));
        // TODO IO-386: Do we still need this check?
        checkEqualSizes(srcFile, destFile, srcFile.length(), destFile.length());

        destFile.setLastModified(newLastModifed);
    }

    /**
     * Checks that two file lengths are equal.
     *
     * @param srcFile Source file.
     * @param destFile Destination file.
     * @param srcLen Source file length.
     * @param dstLen Destination file length
     * @throws IOException Thrown when the given sizes are not equal.
     */
    private static void checkEqualSizes(final File srcFile, final File destFile, final long srcLen, final long dstLen)
            throws IOException {
        if (srcLen != dstLen) {
            throw new IOException("Failed to copy full contents from '" + srcFile + "' to '" + destFile
                    + "' Expected length: " + srcLen + " Actual: " + dstLen);
        }
    }

    /**
     * Writes a String to a file creating the file if it does not exist.
     *
     * @param file     the file to write
     * @param data     the content to write to the file
     * @param charset the charset to use, {@code null} means platform default
     * @param append   if {@code true}, then the String will be added to the
     *                 end of the file rather than overwriting
     * @throws IOException in case of an I/O error
     * @since 2.3
     */
    public static void writeStringToFile(final File file, final String data, final Charset charset,
                                         final boolean append) throws IOException {
        try (OutputStream out = openOutputStream(file, append)) {
            IOUtils.write(data, out, charset);
        }
    }

    /**
     * Opens a {@link FileOutputStream} for the specified file, checking and
     * creating the parent directory if it does not exist.
     * <p>
     * At the end of the method either the stream will be successfully opened,
     * or an exception will have been thrown.
     * </p>
     * <p>
     * The parent directory will be created if it does not exist.
     * The file will be created if it does not exist.
     * An exception is thrown if the file object exists but is a directory.
     * An exception is thrown if the file exists but cannot be written to.
     * An exception is thrown if the parent directory cannot be created.
     * </p>
     *
     * @param file   the file to open for output, must not be {@code null}
     * @param append if {@code true}, then bytes will be added to the
     *               end of the file rather than overwriting
     * @return a new {@link FileOutputStream} for the specified file
     * @throws IOException if the file object is a directory
     * @throws IOException if the file cannot be written to
     * @throws IOException if a parent directory needs creating but that fails
     * @since 2.1
     */
    public static FileOutputStream openOutputStream(final File file, final boolean append) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (file.canWrite() == false) {
                throw new IOException("File '" + file + "' cannot be written to");
            }
        } else {
            final File parent = file.getParentFile();
            if (parent != null) {
                if (!parent.mkdirs() && !parent.isDirectory()) {
                    throw new IOException("Directory '" + parent + "' could not be created");
                }
            }
        }
        return new FileOutputStream(file, append);
    }

    public static void writeLines(File file, String charsetName, Collection<?> lines, boolean append) throws IOException {
        writeLines(file, charsetName, lines, (String)null, append);
    }

    public static void writeLines(File file, String charsetName, Collection<?> lines, String lineEnding, boolean append) throws IOException {
        OutputStream out = new BufferedOutputStream(openOutputStream(file, append));
        Throwable var6 = null;

        try {
            IOUtils.writeLines(lines, lineEnding, out, charsetName);
        } catch (Throwable var15) {
            var6 = var15;
            throw var15;
        } finally {
            if (out != null) {
                if (var6 != null) {
                    try {
                        out.close();
                    } catch (Throwable var14) {
                        var6.addSuppressed(var14);
                    }
                } else {
                    out.close();
                }
            }

        }

    }
}
