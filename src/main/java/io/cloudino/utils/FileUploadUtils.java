package io.cloudino.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 * Wrapper for Apache commons fileupload
 * @author serch
 */
public class FileUploadUtils {

    private static final int MAX_MEMORY_SIZE = 512_000;
    private static final int MAX_FILE_SIZE = 5_000_000;
    private static final DiskFileItemFactory factory = new DiskFileItemFactory();
    private static Logger logger = Logger.getLogger("i.c.u.FileUploadUtils");

    static {
        factory.setSizeThreshold(MAX_MEMORY_SIZE);
    }

    /**
     * Initialize, a temporal directory should be provided
     * @param tmpdir temporal directory
     */
    public static void init(File tmpdir) {
        factory.setRepository(tmpdir);
    }

    /**
     * Copy the Uploaded file to an OutputStream with a maximum size of MAX_FILE_SIZE (5,000,000 bytes)
     * @param request request with the file to upload
     * @param outputStream Stream to put the file into
     * @param fileFieldName name of the form field with the file
     * @return true if everything went OK
     */
    public static boolean saveToOutputStream(HttpServletRequest request,
            OutputStream outputStream, String fileFieldName) {
        return saveToOutputStream(request, outputStream, fileFieldName, MAX_FILE_SIZE);
    }

    /**
     * Copy the Uploaded file to an OutputStream
     * @param request request with the file to upload
     * @param outputStream Stream to put the file into
     * @param fileFieldName name of the form field with the file
     * @param maxFileSize maximum size in bytes of the uploaded file
     * @return true if everything went OK
     */
    public static boolean saveToOutputStream(HttpServletRequest request,
            OutputStream outputStream, String fileFieldName, int maxFileSize) {
        ServletFileUpload upload = new ServletFileUpload(factory);
        upload.setSizeMax(maxFileSize);
        try {
            List<FileItem> items = upload.parseRequest(request);
            Optional<FileItem> oFileItem = items
                    .stream()
                    .filter(
                            fi -> fi.getFieldName().equalsIgnoreCase(fileFieldName))
                    .findFirst();
            if (oFileItem.isPresent()) {
                return processOutputStream(oFileItem.get(), outputStream);
            } else {
                return false;
            }
        } catch (FileUploadException ioe) {
            logger.log(Level.FINE, "Error Uploading a file form web", ioe);
            return false;
        }
    }

    /**
     * Copy the contents of a FileItem to an OutputStream
     * @param fileItem fileItem to read
     * @param outputStream outputStream to write to
     * @return true if OK
     */
    private static boolean processOutputStream(FileItem fileItem, OutputStream outputStream) {
        try (InputStream is = fileItem.getInputStream()) {
            int c;
            byte[] buff = new byte[8192];
            while ((c = is.read(buff)) > -1) {
                outputStream.write(buff, 0, c);
            }
            return true;
        } catch (IOException ioe) {
            logger.log(Level.FINE, "Error processing an uploaded file", ioe);
            return false;
        }
    }

    /**
     * Save an Uploaded file to a known File with a maximum size of MAX_FILE_SIZE (5,000,000 bytes)
     * If it's possible the Uploaded file just will be moved to File, if not it will be copied
     * @param request request with the file to upload
     * @param outputFile File to write the file to
     * @param fileFieldName name of the form field with the file
     * @return true if everything went OK
     */
    public static boolean saveToFile(HttpServletRequest request,
            File outputFile, String fileFieldName) {
        return saveToFile(request, outputFile, fileFieldName, MAX_FILE_SIZE);
    }

    /**
     * Save an Uploaded file to a known File
     * If it's possible the Uploaded file just will be moved to File, if not it will be copied
     * @param request request with the file to upload
     * @param outputFile File to write the file to
     * @param fileFieldName name of the form field with the file
     * @param maxFileSize maximum size in bytes of the uploaded file
     * @return true if everything went OK
     */
    public static boolean saveToFile(HttpServletRequest request,
            File outputFile, String fileFieldName, int maxFileSize) {
        ServletFileUpload upload = new ServletFileUpload(factory);
        upload.setSizeMax(maxFileSize);
        try {
            List<FileItem> items = upload.parseRequest(request);
            Optional<FileItem> oFileItem = items
                    .stream()
                    .filter(
                            fi -> fi.getFieldName().equalsIgnoreCase(fileFieldName))
                    .findFirst();
            oFileItem.ifPresent(fi -> {
                try {
                    fi.write(outputFile);
                } catch (Exception ex) {
                    logger.log(Level.FINE, "Error Writing to a file", ex);
                }
            });
            return oFileItem.isPresent();
        } catch (FileUploadException ioe) {
            logger.log(Level.FINE, "Error Uploading a file form web", ioe);
            return false;
        }
    }
}
