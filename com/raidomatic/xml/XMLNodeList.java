package com.raidomatic.xml;

import java.util.ArrayList;

public class XMLNodeList extends ArrayList<XMLNode> {
	private static final long serialVersionUID = -5345253808779456477L;

	public XMLNodeList() { super(); }
	public XMLNodeList(int initialCapacity) {
		super(initialCapacity);
	}

	@Override
	public XMLNode remove(int i) {
		XMLNode result = super.remove(i);
		if(result != null) {
			result.getParentNode().removeChild(result);
		}
		return result;
	}
}