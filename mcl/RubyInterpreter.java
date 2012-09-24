package mcl;

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
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		PrintWriter out = res.getWriter();
		
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
				
				// Add some important environment variables as constants
				container.put("$session", session);
				container.put("$database", database);
				container.put("$request", req);
				container.put("$response", res);
				
				// Set the writer to be the HTTP output
				container.setWriter(out);
				
				// Execute the code
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
	
	private String strLeft(String input, String delimiter) {
		return input.substring(0, input.indexOf(delimiter));
	}
	private String strRight(String input, String delimiter) {
		return input.substring(input.indexOf(delimiter) + delimiter.length());
	}
	private String strLeftBack(String input, String delimiter) {
		return input.substring(0, input.lastIndexOf(delimiter));
	}
	private String strRightBack(String input, String delimiter) {
		return input.substring(input.lastIndexOf(delimiter) + delimiter.length());
	}
}