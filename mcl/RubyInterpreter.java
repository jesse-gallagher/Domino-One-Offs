package mtc;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import lotus.domino.*;
import sun.misc.BASE64Decoder;

import org.jruby.embed.*;

public class RubyInterpreter extends HttpServlet {
	private static final long serialVersionUID = 2229617989934548785L;
	private ScriptingContainer container;
	
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		
		container = new ScriptingContainer(LocalContextScope.THREADSAFE);
		System.out.println("Loaded container");
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		//res.setContentType("text/plain");
		PrintWriter out = res.getWriter();
		//out.println("Starting");
		//out.println("Request URI: " + req.getRequestURI());
		//out.println("Servlet Path: " + req.getServletPath());
		
		
		try {
			NotesThread.sinitThread();
			// Create a session using the currently-authenticated user
			Session session = NotesFactory.createSession(null, req);
			
			// This servlet is intended to be used with NSF-based .rb file resources
			String dominoURI = "notes://" + req.getRequestURI() + "?OpenFileResource";
			Base obj = session.resolve(dominoURI);
			if(obj instanceof Form) {
				// File resources are presented as Form objects, the worst kind
				Form formObj = (Form)obj;
				Database database = formObj.getParent();
				
				// Find the Form's UNID via its URL and use that to get the file resource as a document
				String universalID = strRightBack(strLeftBack(formObj.getURL(), "?"), "/");
				Document fileResource = database.getDocumentByUNID(universalID);
				String dxl = fileResource.generateXML();
				String rubyCode = new String(new BASE64Decoder().decodeBuffer(strLeft(strRight(dxl, "<filedata>\n"), "\n</filedata>")));
				
				container.put("$session", session);
				container.put("$database", database);
				container.put("$request", req);
				container.put("$response", res);
				
				container.setOutput(out);
				container.setError(out);
				container.runScriptlet(rubyCode);
			}
			
			obj.recycle();
			
			session.recycle();
		} catch(Exception ne) {
			out.println();
			out.println("=======================");
			ne.printStackTrace(out);
		} finally {
			NotesThread.stermThread();
		}
		
	}
	
	public static String strLeft(String input, String delimiter) {
		return input.substring(0, input.indexOf(delimiter));
	}
	public static String strRight(String input, String delimiter) {
		return input.substring(input.indexOf(delimiter) + delimiter.length());
	}
	public static String strLeftBack(String input, String delimiter) {
		return input.substring(0, input.lastIndexOf(delimiter));
	}
	public static String strLeftBack(String input, int chars) {
		return input.substring(0, input.length() - chars);
	}
	public static String strRightBack(String input, String delimiter) {
		return input.substring(input.lastIndexOf(delimiter) + delimiter.length());
	}
	public static String strRightBack(String input, int chars) {
		return input.substring(input.length() - chars);
	}
}