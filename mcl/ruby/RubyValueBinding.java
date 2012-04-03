package mtc.ruby;

import javax.faces.context.FacesContext;
import javax.faces.el.EvaluationException;
import javax.faces.el.PropertyNotFoundException;
import javax.faces.el.ValueBinding;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;

public class RubyValueBinding extends ValueBinding {
	String content;
	
	public RubyValueBinding(String content) {
		this.content = content;
	}
	
	@Override
	public Class getType(FacesContext arg0) throws EvaluationException, PropertyNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getValue(FacesContext arg0) throws EvaluationException, PropertyNotFoundException {
		ScriptingContainer container = new ScriptingContainer(LocalContextScope.SINGLETON);
		return container.runScriptlet(this.content);
	}

	@Override
	public boolean isReadOnly(FacesContext arg0) throws EvaluationException, PropertyNotFoundException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setValue(FacesContext arg0, Object arg1) throws EvaluationException, PropertyNotFoundException {
		// TODO Auto-generated method stub
		
	}

}
