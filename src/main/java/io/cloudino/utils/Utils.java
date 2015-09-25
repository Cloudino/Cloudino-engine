/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package io.cloudino.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.stream.Collectors;
import org.semanticwb.datamanager.DataObject;

/**
 *
 * @author javiersolis
 */
public class Utils {

    private static final int BUFFERSIZE = 8192;

    /**
     * Copies an input stream into an output stream using the buffer size
     * defined by {@code SWBUtils.bufferSize} in the reading/writing operations.
     * <p>
     * Copia un flujo de entrada en uno de salida utilizando el tama&ntilde;o de
     * buffer definido por {@code SWBUtils.bufferSize} en las operaciones de
     * lectura/escritura.</p>
     *
     * @param in the input stream to read from
     * @param out the output stream to write to
     * @throws IOException if either the input or the output stream is
     * {@code null}.
     * <p>
     * Si el flujo de entrada o el de salida es {@code null}.</p>
     */
    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        copyStream(in, out, BUFFERSIZE);
    }

    /**
     * Copies an input stream into an output stream using the specified buffer
     * size in the reading/writing operations.
     * <p>
     * Copia un flujo de entrada en uno de salida utilizando el tama&ntilde;o de
     * buffer especificado en las operaciones de lectura/escritura.</p>
     *
     * @param in the input stream to read from
     * @param out the output stream to write to
     * @param bufferSize the number of bytes read/writen at the same time in
     * each I/O operation
     * @throws IOException if either the input or the output stream is
     * {@code null}.
     * <p>
     * Si el flujo de entrada o el de salida es {@code null}.</p>
     */
    public static void copyStream(InputStream in, OutputStream out, int bufferSize) throws IOException 
    {
        if (in == null) {
            throw new IOException("Input Stream null");
        }
        if (out == null) {
            throw new IOException("Ouput Stream null");
        }
        byte[] bfile = new byte[bufferSize];
        int x;
        while ((x = in.read(bfile, 0, bufferSize)) > -1) {
            out.write(bfile, 0, x);
        }
        in.close();
        out.flush();
        out.close();
    }

    public static String textInputStreamToString(final InputStream is, final String charset) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, charset))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
        
    public static DataObject readJsonFromUrl(String url) throws IOException {
        InputStream is = new URL(url).openStream();
        String jsonText = textInputStreamToString(is, "UTF-8");
        return (DataObject) DataObject.parseJSON(jsonText);
    }

}
