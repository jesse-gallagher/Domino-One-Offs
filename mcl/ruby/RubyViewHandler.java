package mtc.ruby;

import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

import com.ibm.xsp.application.ApplicationEx;
import com.ibm.xsp.factory.FactoryLookup;

public class RubyViewHandler extends com.ibm.xsp.application.ViewHandlerExImpl {

	public RubyViewHandler(ViewHandler arg0) {
		super(arg0);
	}
	
	@SuppressWarnings({ "deprecation" })
	@Override
	public UIViewRoot createView(FacesContext context, String paramString) {
		ApplicationEx app = (ApplicationEx)context.getApplication();
		FactoryLookup facts = app.getFactoryLookup();
		
		RubyBindingFactory rfac = new mtc.ruby.RubyBindingFactory();
		if(facts.getFactory(rfac.getPrefix()) == null) {
			//System.out.println("Adding ruby interpreter");
			facts.setFactory(rfac.getPrefix(), rfac);
		}
		
		return super.createView(context, paramString);
	}

}