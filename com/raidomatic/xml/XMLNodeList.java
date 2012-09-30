package com.raidomatic.xml;

/*
 * � Copyright Jesse Gallagher, 2012
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at:
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
 * implied. See the License for the specific language governing 
 * permissions and limitations under the License.
 * 
 * Author: Jesse Gallagher
 */

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