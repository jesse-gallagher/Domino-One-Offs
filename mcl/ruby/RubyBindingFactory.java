package mtc.ruby;

import javax.faces.application.Application;
import javax.faces.el.MethodBinding;
import javax.faces.el.ValueBinding;

import com.ibm.xsp.util.ValueBindingUtil;
import com.ibm.xsp.binding.BindingFactory;

public class RubyBindingFactory implements BindingFactory {

	@SuppressWarnings("unchecked")
	public MethodBinding createMethodBinding(Application arg0, String arg1, Class[] arg2) {
		String script = ValueBindingUtil.parseSimpleExpression(arg1);
		return new RubyMethodBinding(script);
	}

	public ValueBinding createValueBinding(Application arg0, String arg1) {
		String script = ValueBindingUtil.parseSimpleExpression(arg1);
		return new RubyValueBinding(script);
	}

	public String getPrefix() {
		return "ruby";
	}
	
}