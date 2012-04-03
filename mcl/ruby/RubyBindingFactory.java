package mtc.ruby;

import javax.faces.application.Application;
import javax.faces.el.MethodBinding;
import javax.faces.el.ValueBinding;

import com.ibm.xsp.binding.BindingFactory;

public class RubyBindingFactory implements BindingFactory {

	public MethodBinding createMethodBinding(Application arg0, String arg1, Class[] arg2) {
		// Not yet implemented
		System.out.println("Called ruby:createMethodBinding");
		return null;
	}

	public ValueBinding createValueBinding(Application arg0, String arg1) {
		return new RubyValueBinding(arg1.substring(7, arg1.length()-1));
	}

	public String getPrefix() {
		return "ruby";
	}
	
}