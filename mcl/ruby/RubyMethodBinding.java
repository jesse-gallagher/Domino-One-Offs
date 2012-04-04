package mtc.ruby;

import javax.faces.context.FacesContext;
import javax.faces.el.EvaluationException;
import javax.faces.el.MethodNotFoundException;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;

import com.ibm.xsp.binding.MethodBindingEx;

public class RubyMethodBinding extends MethodBindingEx {
	private String content;

	public RubyMethodBinding() {
		super();
	}

	public RubyMethodBinding(String expression) {
		super();
		this.content = expression;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Class getType(FacesContext arg0) throws MethodNotFoundException {
		//return Object.class;
		return null;
	}

	@Override
	public Object invoke(FacesContext context, Object[] arg1) throws EvaluationException, MethodNotFoundException {
		ScriptingContainer container = new ScriptingContainer(LocalContextScope.CONCURRENT);
		
		container.put("$facesContext", context);
		// Define a generic method_missing that looks to the surrounding context for variables
		container.runScriptlet(
			"def method_missing(name, *args)\n" +
			"	if args.size == 0\n" +
			"		val = $facesContext.get_application.get_variable_resolver.resolve_variable($facesContext, name.to_s)\n" +
			"		if val != nil\n" +
			"			return val\n" +
			"		end\n" +
			"	end\n" +
			"	super\n" +
			"end"
		);
		
		Object result = container.runScriptlet(this.content);
		container.terminate();
		return result;
	}

}
