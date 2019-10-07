package io.cloudino.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 *
 * @author serch
 */
@WebServlet(name = "CloudinoAPIServlet", urlPatterns = {"/api/*"})
public class CloudinoAPIServlet extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException 
    {
        try
        {
            HttpServletResponseWrapper wrapper=new HttpServletResponseWrapper(response){
                
                ServletOutputStream out=null;
                
                @Override
                public void setContentType(String type) {
                    response.setContentType(type);
                }         

                @Override
                public void setContentLength(int len) {
                    response.setContentLength(len); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public void setContentLengthLong(long len) {
                    response.setContentLengthLong(len); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public void setHeader(String name, String value) {
                    response.setHeader(name, value);
                }

                @Override
                public void setStatus(int sc) {
                    response.setStatus(sc); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                @Deprecated
                public void setStatus(int sc, String sm) {
                    response.setStatus(sc, sm); //To change body of generated methods, choose Tools | Templates.
                }
                
                @Override
                public void sendError(int sc) throws IOException {
                    response.sendError(sc); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public void sendError(int sc, String msg) throws IOException {
                    response.sendError(sc, msg); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public void setCharacterEncoding(String charset) {
                    response.setCharacterEncoding(charset); //To change body of generated methods, choose Tools | Templates.
                }

                @Override
                public ServletOutputStream getOutputStream() throws IOException {
                    if(out==null)
                    {
                        out=response.getOutputStream();
                    }
                    return out; //To change body of generated methods, choose Tools | Templates.
                }
                
            };
            request.getServletContext().getRequestDispatcher("/work/api/api.jsp").include(request, wrapper);
        }catch(ServletException e)
        {
            e.printStackTrace();
            response.sendError(404,"Service not found...");
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "CloudinoAPIServlet";
    }// </editor-fold>

}
