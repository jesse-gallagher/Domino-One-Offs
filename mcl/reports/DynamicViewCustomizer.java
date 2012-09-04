/*
 * Extends objects from https://svn-166.openntf.org/svn/xpages/extlib/eclipse/plugins/com.ibm.xsp.extlib.domino/src/com/ibm/xsp/extlib/component/dynamicview/ViewDesign.java
 */

package mcl.reports;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;

import lotus.domino.ColorObject;
import lotus.domino.Database;
import lotus.domino.DateTime;
import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.Session;
import lotus.domino.View;
import lotus.domino.ViewColumn;
import mcl.JSFUtil;

import com.ibm.commons.util.SystemCache;
import com.ibm.xsp.FacesExceptionEx;
import com.ibm.xsp.extlib.builder.ControlBuilder.IControl;
import com.ibm.xsp.extlib.component.dynamicview.UIDynamicViewPanel;
import com.ibm.xsp.extlib.component.dynamicview.ViewColumnConverter;
import com.ibm.xsp.extlib.component.dynamicview.ViewDesign;
import com.ibm.xsp.extlib.component.dynamicview.DominoDynamicColumnBuilder.DominoViewCustomizer;
import com.ibm.xsp.extlib.component.dynamicview.ViewDesign.ColumnDef;
import com.ibm.xsp.extlib.component.dynamicview.ViewDesign.DefaultColumnDef;
import com.ibm.xsp.extlib.component.dynamicview.ViewDesign.DefaultViewDef;
import com.ibm.xsp.extlib.component.dynamicview.ViewDesign.DefaultViewFactory;
import com.ibm.xsp.extlib.component.dynamicview.ViewDesign.ViewDef;
import com.ibm.xsp.extlib.component.dynamicview.ViewDesign.ViewFactory;
import com.ibm.xsp.model.domino.wrapped.DominoViewEntry;
import com.raidomatic.xml.XMLDocument;
import com.raidomatic.xml.XMLNode;


public class DynamicViewCustomizer extends DominoViewCustomizer {
	UIDynamicViewPanel panel;

	@Override
	public ViewFactory getViewFactory() {
		return new DynamicViewFactory();
	}

	public class DynamicViewFactory extends DefaultViewFactory {
		private static final long serialVersionUID = 123034173761337005L;
		private transient final SystemCache views = new SystemCache("View Definition", 16, "xsp.extlib.viewdefsize");

		@Override
		public ViewDef getViewDef(View view) {
			if(view == null) {
				return null;
			}
			try {
				String viewKey = ViewDesign.getViewKey(view);
				DefaultViewDef viewDef = (DefaultViewDef)this.views.get(viewKey);
				if(viewDef == null) {
					// Read the view
					viewDef = new DefaultViewDef();
					if(view.isHierarchical()) { viewDef.flags |= DefaultViewDef.FLAG_HIERARCHICAL; }
					if(view.isCategorized()) { viewDef.flags |= DefaultViewDef.FLAG_CATEGORIZED; }

					viewDef.columns.addAll(this.getViewColumnInformation(view));
				}
				return viewDef;
			} catch(Exception ex) {
				throw new FacesExceptionEx(ex, "Error while accessing view {0}", view.toString());
			}
		}

		@SuppressWarnings("unchecked")
		private List<ColumnDef> getViewColumnInformation(View view) throws Exception {
			Database database = view.getParent();

			/* Generate the DXL */
			Document viewDoc = database.getDocumentByUNID(view.getUniversalID());
			String dxl = viewDoc.generateXML();
			InputStream is = new ByteArrayInputStream(dxl.getBytes(Charset.forName("UTF-8")));
			XMLDocument dxlDoc = new XMLDocument();
			dxlDoc.loadInputStream(is);
			viewDoc.recycle();

			/*
			 * Fetch both types of column information since some properties are
			 * much easier to deal with via the standard API
			 */
			List<ViewColumn> viewColumns = view.getColumns();
			List<XMLNode> dxlColumns = dxlDoc.selectNodes("//column");

			// Figure out if we're going to extend the last column
			boolean extendLastColumn = dxlDoc.selectSingleNode("//view").getAttribute("extendlastcolumn").equals("true");

			Document contextDoc = database.createDocument();
			List<ColumnDef> columns = new Vector<ColumnDef>();
			String activeColorColumn = "";
			for(int i = 0; i < dxlColumns.size(); i++) {
				XMLNode columnNode = dxlColumns.get(i);
				ViewColumn viewColumn = viewColumns.get(i);

				ExtendedColumnDef column = new ExtendedColumnDef();
				column.replicaId = view.getParent().getReplicaID();

				if(columnNode.getAttribute("hidden").equals("true")) {
					column.flags |= DefaultColumnDef.FLAG_HIDDEN;
				} else {
					// Check to see if it's hidden by a hide-when formula
					XMLNode hideWhen = columnNode.selectSingleNode("code[@event='hidewhen']");
					if(hideWhen != null) {
						if(hideWhen.getAttribute("enabled") == null || !hideWhen.getAttribute("enabled").equals("false")) {
							String hideWhenFormula = hideWhen.getText();
							if(hideWhenFormula.length() > 0) {
								List<Object> evalResult = JSFUtil.getSession().evaluate(hideWhenFormula, contextDoc);
								if(evalResult.size() > 0 && evalResult.get(0) instanceof Double && (Double)evalResult.get(0) == 1) {
									column.flags |= DefaultColumnDef.FLAG_HIDDEN;
								}
							}
						}
					}
				}

				column.name = columnNode.getAttribute("itemname");

				if(columnNode.getAttribute("showascolor").equals("true")) {
					activeColorColumn = column.name;
				}
				column.colorColumn = activeColorColumn;

				// Get the header information
				XMLNode header = columnNode.selectSingleNode("columnheader");
				column.title = columnNode.selectSingleNode("columnheader").getAttribute("title");
				if(header.getAttribute("align").equals("center")) {
					column.flags |= DefaultColumnDef.FLAG_HALIGNCENTER;
				} else if(header.getAttribute("align").equals("right")) {
					column.flags |= DefaultColumnDef.FLAG_HALIGNRIGHT;
				}

				column.width = new Float(columnNode.getAttribute("width")).intValue();
				column.actualWidth = Double.parseDouble(columnNode.getAttribute("width"));

				if(columnNode.getAttribute("responsesonly").equals(true)) {
					column.flags |= DefaultColumnDef.FLAG_RESPONSE;
				}
				if(columnNode.getAttribute("categorized").equals("true")) {
					column.flags |= DefaultColumnDef.FLAG_CATEGORIZED;
				}
				if(columnNode.getAttribute("sort").length() > 0) {
					column.flags |= DefaultColumnDef.FLAG_SORTED;
				}
				if(columnNode.getAttribute("resort").equals("ascending") || columnNode.getAttribute("resort").equals("both")) {
					column.flags |= DefaultColumnDef.FLAG_RESORTASC;
				}
				if(columnNode.getAttribute("resort").equals("descending") || columnNode.getAttribute("resort").equals("both")) {
					column.flags |= DefaultColumnDef.FLAG_RESORTDESC;
				}
				if(columnNode.getAttribute("align").equals("center")) {
					column.flags |= DefaultColumnDef.FLAG_ALIGNCENTER;
				} else if(columnNode.getAttribute("align").equals("right")) {
					column.flags |= DefaultColumnDef.FLAG_ALIGNRIGHT;
				}
				if(columnNode.getAttribute("showaslinks").equals("true")) {
					column.flags |= DefaultColumnDef.FLAG_LINK | DefaultColumnDef.FLAG_ONCLICK | DefaultColumnDef.FLAG_CHECKBOX;
				}

				column.numberFmt = viewColumn.getNumberFormat();
				column.numberDigits = viewColumn.getNumberDigits();
				column.numberAttrib = viewColumn.getNumberAttrib();
				if(viewColumn.isNumberAttribParens()) {
					column.flags |= DefaultColumnDef.FLAG_ATTRIBPARENS;
				}
				if(viewColumn.isNumberAttribPercent()) {
					column.flags |= DefaultColumnDef.FLAG_ATTRIBPERCENT;
				}
				if(viewColumn.isNumberAttribPunctuated()) {
					column.flags |= DefaultColumnDef.FLAG_ATTRIBPUNC;
				}
				column.timeDateFmt = viewColumn.getTimeDateFmt();
				column.dateFmt = viewColumn.getDateFmt();
				column.timeFmt = viewColumn.getTimeFmt();
				column.timeZoneFmt = viewColumn.getTimeZoneFmt();
				column.listSep = viewColumn.getListSep();

				column.fontFace = viewColumn.getFontFace();
				column.fontStyle = viewColumn.getFontStyle();
				column.fontPointSize = viewColumn.getFontPointSize();
				column.fontColor = viewColumn.getFontColor();

				column.headerFontFace = viewColumn.getHeaderFontFace();
				column.headerFontStyle = viewColumn.getHeaderFontStyle();
				column.headerFontPointSize = viewColumn.getHeaderFontPointSize();
				column.headerFontColor = viewColumn.getHeaderFontColor();

				if(columnNode.getAttribute("showasicons").equals("true")) {
					column.flags |= DefaultColumnDef.FLAG_ICON;
				}
				if(columnNode.getAttribute("twisties").equals("true")) {
					column.flags |= DefaultColumnDef.FLAG_INDENTRESP;
				}

				// Find any twistie image
				XMLNode twistieImage = columnNode.selectSingleNode("twistieimage/imageref");
				if(twistieImage != null) {
					if(twistieImage.getAttribute("database").equals("0000000000000000")) {
						column.twistieReplicaId = database.getReplicaID();
					} else {
						column.twistieReplicaId = twistieImage.getAttribute("database");
					}

					// Make sure that the referenced database is available on
					// the current server
					boolean setTwistie = true;
					if(!column.twistieReplicaId.equalsIgnoreCase(database.getReplicaID())) {
						Database twistieDB = JSFUtil.getSession().getDatabase("", "");
						twistieDB.openByReplicaID("", column.twistieReplicaId);
						if(!twistieDB.isOpen()) {
							setTwistie = false;
						}
						twistieDB.recycle();
					}
					if(setTwistie) {
						column.twistieImage = twistieImage.getAttribute("name");
					}

				}

				// Support extending the column width to the full window.
				// In the client, "extend last column" takes priority
				if(extendLastColumn && i == dxlColumns.size() - 1) {
					column.extendColumn = true;
				} else if(!extendLastColumn && columnNode.getAttribute("extwindowwidth").equals("true")) {
					column.extendColumn = true;
				}

				columns.add(column);

				viewColumn.recycle();
			}
			contextDoc.recycle();

			database.recycle();

			return columns;
		}

		public class ExtendedColumnDef extends DefaultColumnDef implements Serializable {
			private static final long serialVersionUID = 5158008403553374867L;

			public String colorColumn;
			public String twistieImage = "";
			public String twistieReplicaId = "";
			public boolean extendColumn = false;
			public String replicaId = "";
			public double actualWidth;

			public String fontFace = "";
			public int fontPointSize = 0;
			public int fontStyle = 0;
			public int fontColor = 0;

			public String headerFontFace = "";
			public int headerFontPointSize = 0;
			public int headerFontStyle = 0;
			public int headerFontColor = 0;
		}
	}

	@Override
	public boolean createColumns(FacesContext context, UIDynamicViewPanel panel, ViewFactory f) {
		// All I care about here is getting the panel for later
		this.panel = panel;
		return false;
	}

	@Override
	public void afterCreateColumn(FacesContext context, int index, ColumnDef colDef, IControl column) {
		// Patch in a converter to handle special text and icon columns
		UIDynamicViewPanel.DynamicColumn col = (UIDynamicViewPanel.DynamicColumn)column.getComponent();
		if(colDef.isIcon()) {
			// For icons, override the default behavior so it can handle
			// string-based ones
			col.setValueBinding("iconSrc", null);
			col.setDisplayAs("");
			col.setConverter(new IconColumnConverter(null, colDef, this.panel));
		} else {
			col.setConverter(new ExtendedViewColumnConverter(null, colDef, this.panel));
		}

		// Apply a general style class to indicate that it's not just some
		// normal view panel column
		// Many style attributes will be class-based both for flexibility and
		// because headers can't have style applied directly
		String styleClass = " notesViewColumn";
		String headerStyleClass = "";

		// Add an extra class for category columns
		if(colDef.isCategorized()) {
			styleClass += " notesViewCategory";
			// col.setStyleClass((col.getStyleClass() == null ? "" :
			// col.getStyleClass()) + " categoryColumn");
		}

		// We'll handle escaping the HTML manually, to support
		// [<b>Notes-style</b>] pass-through-HTML and icon columns
		col.setContentType("html");

		// Deal with any twistie images and color columns
		if(colDef instanceof DynamicViewFactory.ExtendedColumnDef) {
			DynamicViewFactory.ExtendedColumnDef extColDef = (DynamicViewFactory.ExtendedColumnDef)colDef;

			if(extColDef.twistieImage.length() > 0) {
				// Assume that it's a multi-image well for now
				col.setCollapsedImage("/.ibmxspres/domino/__" + extColDef.twistieReplicaId + ".nsf/" + extColDef.twistieImage.replaceAll("\\\\", "/") + "?Open&ImgIndex=2");
				col.setExpandedImage("/.ibmxspres/domino/__" + extColDef.twistieReplicaId + ".nsf/" + extColDef.twistieImage.replaceAll("\\\\", "/") + "?Open&ImgIndex=1");
			}

			// The style applies to the contents of the column as well as the
			// column itself, which messes with icon columns
			// For now, don't apply it at all to those columns
			String style = "";
			// if(!extColDef.extendColumn && !extColDef.isCategorized()) {
			if(!extColDef.extendColumn) {
				style = "max-width: " + (extColDef.actualWidth * extColDef.fontPointSize * 1.3) + "px; min-width: " + (extColDef.actualWidth * extColDef.fontPointSize * 1.3) + "px";
				// style = "width: " + (extColDef.width * 11) + "px";
				// } else if(extColDef.extendColumn &&
				// !extColDef.isCategorized()) {
			} else {
				style = "width: 100%";
			}

			// Check for left or right alignment
			switch(extColDef.getAlignment()) {
			case ViewColumn.ALIGN_CENTER:
				// style += "; text-align: center";
				styleClass += " notesViewAlignCenter";
				break;
			case ViewColumn.ALIGN_RIGHT:
				// style += "; text-align: right";
				styleClass += " notesViewAlignRight";
				break;
			}

			// Add font information
			styleClass += this.fontStyleToStyleClass(extColDef.fontStyle);
			headerStyleClass += this.fontStyleToStyleClass(extColDef.headerFontStyle);
			// style += "; font-face: '" + extColDef.fontFace + "'";
			style += "; color: " + this.notesColorToCSS(extColDef.fontColor);

			if(extColDef.colorColumn.length() > 0) {
				String styleFormula = "#{javascript:'" + style.replace("'", "\\'") + ";' + " + this.getClass().getName() + ".colorColumnToStyle(" + this.panel.getVar() + ".getColumnValue('"
				+ extColDef.colorColumn + "'))}";
				ValueBinding styleBinding = context.getApplication().createValueBinding(styleFormula);
				col.setValueBinding("style", styleBinding);
			} else {
				col.setStyle(style);
			}

		}
		col.setStyleClass((col.getStyleClass() == null ? "" : col.getStyleClass()) + styleClass);
		col.setHeaderClass((col.getHeaderClass() == null ? "" : col.getHeaderClass()) + headerStyleClass);
	}

	public class ExtendedViewColumnConverter extends ViewColumnConverter {
		ColumnDef colDef;
		UIDynamicViewPanel panel;

		// For loading the state
		public ExtendedViewColumnConverter() {}

		public ExtendedViewColumnConverter(ViewDef viewDef, ColumnDef colDef, UIDynamicViewPanel panel) {
			super(viewDef, colDef);
			this.colDef = colDef;
			this.panel = panel;
		}

		@Override
		public String getAsString(FacesContext context, UIComponent component, Object value) {
			// First, apply any column-color info needed
			DominoViewEntry entry = this.resolveViewEntry(context, this.panel.getVar());
			try {
				if(value instanceof DateTime) {
					return this.getValueDateTimeAsString(context, component, ((DateTime)value).toJavaDate());
				}
				if(value instanceof Date) {
					return this.getValueDateTimeAsString(context, component, (Date)value);
				}
				if(value instanceof Number) {
					return this.getValueNumberAsString(context, component, (Number)value);
				}
			} catch(NotesException ex) {}

			String stringValue = value.toString();

			try {
				stringValue = JSFUtil.specialTextDecode(stringValue, entry);
			} catch(NotesException ne) {}

			// Process the entry as Notes-style pass-through-HTML
			if(!entry.isCategory()) {
				stringValue = this.handlePassThroughHTML(stringValue);
			}

			// Add in some text for empty categories
			if(entry.isCategory() && stringValue.length() == 0) {
				stringValue = "(Not Categorized)";
			}

			// Include a &nbsp; to avoid weird styling problems when the content
			// itself is empty or not visible
			return stringValue;
		}

		// @Override public String getAsString(FacesContext context, UIComponent
		// component, Object value) { return this.getValueAsString(context,
		// component, value); }

		private String handlePassThroughHTML(String cellData) {
			if(cellData.contains("[<") && cellData.contains(">]")) {
				String[] cellChunks = cellData.split("\\[\\<", -2);
				cellData = "";
				for(String chunk : cellChunks) {
					if(chunk.contains(">]")) {
						String[] smallChunks = chunk.split(">]", -2);
						cellData += "<" + smallChunks[0] + ">" + JSFUtil.xmlEncode(smallChunks[1]);
					} else {
						cellData += JSFUtil.xmlEncode(chunk);
					}
				}
			} else {
				cellData = JSFUtil.xmlEncode(cellData);
			}
			return cellData;
		}

		private DominoViewEntry resolveViewEntry(FacesContext context, String var) {
			return (DominoViewEntry)context.getApplication().getVariableResolver().resolveVariable(context, var);
		}

		@Override
		public Object saveState(FacesContext context) {
			Object[] superState = (Object[])super.saveState(context);
			Object[] state = new Object[5];
			state[0] = superState[0];
			state[1] = superState[1];
			state[2] = superState[2];
			state[3] = this.colDef;
			state[4] = this.panel;
			return state;
		}

		@Override
		public void restoreState(FacesContext context, Object value) {
			super.restoreState(context, value);
			Object[] state = (Object[])value;
			this.colDef = (ColumnDef)state[3];
			this.panel = (UIDynamicViewPanel)state[4];
		}
	}

	public class IconColumnConverter extends ViewColumnConverter {
		ColumnDef colDef;
		UIDynamicViewPanel panel;

		// For loading the state
		public IconColumnConverter() {}

		public IconColumnConverter(ViewDef viewDef, ColumnDef colDef, UIDynamicViewPanel panel) {
			super(viewDef, colDef);
			this.colDef = colDef;
			this.panel = panel;
		}

		@SuppressWarnings("unchecked")
		@Override
		public String getAsString(FacesContext context, UIComponent component, Object value) {
			List<Object> listValue;
			if(value instanceof List) {
				listValue = (List<Object>)value;
			} else {
				listValue = new Vector<Object>();
				listValue.add(value);
			}
			StringBuilder result = new StringBuilder();

			result.append("<span style='white-space: nowrap'>");
			for(Object node : listValue) {
				if(node instanceof Double) {
					result.append("<img class='notesViewIconStandard' src='/icons/vwicn");
					Double num = (Double)node;
					// /icons/vwicn999.gif
					if(num < 10) {
						result.append("00");
					} else if(num < 100) {
						result.append("0");
					}
					result.append(num.intValue());
					result.append(".gif' />");
				} else {
					if(String.valueOf(value).length() > 0 && !String.valueOf(value).equals("null")) {
						DynamicViewFactory.ExtendedColumnDef col = (DynamicViewFactory.ExtendedColumnDef)this.colDef;
						try {
							result.append("<img class='notesViewIconCustom' src='");
							result.append("/__" + col.replicaId + ".nsf/" + java.net.URLEncoder.encode(String.valueOf(node), "UTF-8"));
							result.append("' />");
						} catch(Exception e) {}
					}
				}
			}
			result.append("</span>");

			return result.toString();
		}

		@Override
		public Object saveState(FacesContext context) {
			Object[] superState = (Object[])super.saveState(context);
			Object[] state = new Object[5];
			state[0] = superState[0];
			state[1] = superState[1];
			state[2] = superState[2];
			state[3] = this.colDef;
			state[4] = this.panel;
			return state;
		}

		@Override
		public void restoreState(FacesContext context, Object value) {
			super.restoreState(context, value);
			Object[] state = (Object[])value;
			this.colDef = (ColumnDef)state[3];
			this.panel = (UIDynamicViewPanel)state[4];
		}

	}

	@SuppressWarnings("unchecked")
	public static String colorColumnToStyle(Object colorValuesObj) {
		String cellStyle = "";
		if(colorValuesObj instanceof List) {
			List<Double> colorValues = (List<Double>)colorValuesObj;
			if(colorValues.size() > 3) {
				// Then we have a background color
				if(colorValues.get(0) != -1) {
					// Then the background is not pass-through
					cellStyle = "background-color: rgb(" + colorValues.get(0).intValue() + ", " + colorValues.get(1).intValue() + ", " + colorValues.get(2).intValue() + ");";
				} else {
					cellStyle = "";
				}
				if(colorValues.get(3) != -1) {
					// Then main color is not pass-through
					cellStyle += "color: rgb(" + colorValues.get(3).intValue() + ", " + colorValues.get(4).intValue() + ", " + colorValues.get(5).intValue() + ");";
				}
			} else {
				// Then it's just a text color
				if(colorValues.get(0) != -1) {
					cellStyle += "color: rgb(" + colorValues.get(0).intValue() + ", " + colorValues.get(1).intValue() + ", " + colorValues.get(2).intValue() + ");";
				}
			}
		}
		return cellStyle;
	}

	private String fontStyleToStyleClass(int fontStyle) {
		StringBuilder result = new StringBuilder();

		if((fontStyle & ViewColumn.FONT_PLAIN) != 0) {
			result.append(" notesViewPlain");
		}
		if((fontStyle & ViewColumn.FONT_BOLD) != 0) {
			result.append(" notesViewBold");
		}
		if((fontStyle & ViewColumn.FONT_UNDERLINE) != 0) {
			result.append(" notesViewUnderline");
		}
		if((fontStyle & ViewColumn.FONT_STRIKETHROUGH) != 0) {
			result.append(" notesViewStrikethrough");
		}
		if((fontStyle & ViewColumn.FONT_ITALIC) != 0) {
			result.append(" notesViewItalic");
		}

		return result.toString();
	}

	private String notesColorToCSS(int notesColor) {
		try {
			Session session = JSFUtil.getSession();
			ColorObject colorObject = session.createColorObject();
			colorObject.setNotesColor(notesColor);

			StringBuilder result = new StringBuilder();
			result.append("rgb(");
			result.append(colorObject.getRed());
			result.append(",");
			result.append(colorObject.getGreen());
			result.append(",");
			result.append(colorObject.getBlue());
			result.append(")");

			return result.toString();
		} catch(NotesException ne) {
			return "";
		}
	}
}