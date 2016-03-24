/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.cloudino.servlet;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 *
 * @author javiersolis
 */
public class ResponseWrapper extends HttpServletResponseWrapper {
    private CharArrayWriter output = new CharArrayWriter();

    public ResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public String toString() {
        return output.toString();
    }

    @Override
    public PrintWriter getWriter() {
        return new PrintWriter(output);
    }
}
