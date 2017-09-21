package org.adara.api;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;
/*
 * author kbatharia
 */
public class CustomPrintWriter {
	private  HttpServletResponse response;
	private  PrintWriter out;
	
	public CustomPrintWriter(HttpServletResponse response){
		
		this.response=response;
	}
	
	
	public PrintWriter getPrintWriter() throws IOException {
		return response.getWriter();
	}
}
