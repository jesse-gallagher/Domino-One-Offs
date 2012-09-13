package mcl;

import java.util.*;
import java.io.Serializable;
import com.ibm.xsp.model.*;
import lombok.Delegate;
import lotus.domino.*;

@SuppressWarnings("unchecked")
public class SortableMapView extends TabularDataModel implements Serializable, TabularDataSource, List<Map<String, Comparable>> {
	private static final long serialVersionUID = 3475977562822554265L;

	// May as well make it List-compatible
	private interface MapList extends List<Map<String, Comparable>> { }
	private interface MapListExcludes { public boolean add(Map<String, Comparable> element); }
	@Delegate(types=MapList.class, excludes=MapListExcludes.class)
	private List<Map<String, Comparable>> data = new ArrayList<Map<String, Comparable>>();

	private List<Map<String, Comparable>> originalData = null;

	private String sortColumn = "";
	private String sortOrder = "";

	public SortableMapView() { super(); }
	public SortableMapView(View view) throws NotesException {
		ViewEntryCollection entries = view.getAllEntries();
		processEntryCollection(entries);
		entries.recycle();
	}
	public SortableMapView(ViewEntryCollection entries) throws NotesException {
		processEntryCollection(entries);
	}

	public boolean add(Map<String, Comparable> element) {
		return this.data.add(new FakeEntryData(element));
	}
	public boolean add(ViewEntry entry, Map<Integer, String> columnNameMap) throws NotesException {
		Map<String, Comparable> result = new HashMap<String, Comparable>();

		result.put("universalID", entry.getUniversalID());
		entry.setPreferJavaDates(true);
		List<Object> columnValues = entry.getColumnValues();
		for(int i = 0; i < columnValues.size(); i++) {
			if(columnValues.get(i) instanceof Comparable) {
				result.put(columnNameMap.get(i), (Comparable)columnValues.get(i));
			} else {
				result.put(columnNameMap.get(i), columnValues.get(i).toString());
			}
		}

		return this.add(result);
	}

	@Override
	public int getRowCount() { return this.data.size(); }

	@Override
	public Object getRowData() { return this.data.get(this.getRowIndex()); }

	@Override
	public boolean isColumnSortable(String paramString) { return true; }

	@Override
	public int getResortType(String paramString) { return TabularDataModel.RESORT_BOTH; }

	@Override
	public void setResortOrder(String columnName, String sortOrder) {
		// If this is our first sort, copy the original order
		if(this.originalData == null) {
			this.originalData = new ArrayList<Map<String, Comparable>>(this.data);
		}

		if(!columnName.equals(this.sortColumn)) {
			// Switching columns means switch back to ascending by default
			this.sortOrder = sortOrder.equals("descending") ? "descending" : "ascending";
			Collections.sort(this.data, new MapComparator(columnName, this.sortOrder.equals("ascending")));
			this.sortColumn = columnName;
		} else {
			this.sortColumn = columnName;
			if(sortOrder.equals("ascending") || (sortOrder.equals("toggle") && this.sortOrder.length() == 0)) {
				this.sortOrder = "ascending";
				Collections.sort(this.data, new MapComparator(columnName, true));
			} else if(sortOrder.equals("descending") || (sortOrder.equals("toggle") && this.sortOrder.equals("ascending"))) {
				this.sortOrder = "descending";
				Collections.sort(this.data, new MapComparator(columnName, false));
			} else {
				this.sortOrder = "";
				this.data = new ArrayList<Map<String, Comparable>>(this.originalData);
			}
		}
	}

	@Override
	public int getResortState(String paramString) {
		return this.sortOrder.equals("ascending") ? TabularDataModel.RESORT_ASCENDING :
			this.sortOrder.equals("descending") ? TabularDataModel.RESORT_DESCENDING :
				TabularDataModel.RESORT_NONE;
	}

	@Override
	public String getResortColumn() {
		return this.sortColumn;
	}

	private void processEntryCollection(ViewEntryCollection entries) throws NotesException {
		View view = entries.getParent();
		Map<Integer, String> columnNameMap = new HashMap<Integer, String>();
		for(ViewColumn column : (List<ViewColumn>)view.getColumns()) {
			if(column.getColumnValuesIndex() < 65535) {
				columnNameMap.put(column.getColumnValuesIndex(), column.getItemName());
			}
		}

		ViewEntry entry = entries.getFirstEntry();
		while(entry != null) {
			this.add(entry, columnNameMap);

			ViewEntry tempEntry = entry;
			entry = entries.getNextEntry(entry);
			tempEntry.recycle();
		}

		entries.recycle();
	}

	// View Panels know how to deal with ViewRowData better than Maps, apparently, so just pass through
	//  the ViewRowData methods to their Map equivalents
	private class FakeEntryData extends HashMap<String, Comparable> implements ViewRowData {
		private static final long serialVersionUID = 5946100397649532083L;
		private String universalID;

		public FakeEntryData() { super(); }
		public FakeEntryData(Map<String, Comparable> original) { super(original); }

		public Object getColumnValue(String arg0) { return this.get(arg0); }
		public Object getValue(String arg0) { return this.get(arg0); }
		public ColumnInfo getColumnInfo(String arg0) { return null; }
		public String getOpenPageURL(String arg0, boolean arg1) { return null; }
		public boolean isReadOnly(String arg0) { return false; }
		public String getUniversalID() { return this.universalID; }
		public void setUniversalID(String universalID) { this.universalID = universalID; }

		public void setColumnValue(String arg0, Object arg1) {
			if(!(arg1 instanceof Comparable)) {
				this.put(arg0, arg1.toString());
			} else {
				this.put(arg0, (Comparable)arg1);
			}
		}
	}

	// A basic class to compare two Maps by a given comparable key common in each,
	//  allowing for descending order
	private class MapComparator implements Comparator<Map<String, Comparable>> {
		private String key;
		private boolean ascending;

		public MapComparator(String key, boolean ascending) {
			this.key = key;
			this.ascending = ascending;
		}

		public int compare(Map<String, Comparable> o1, Map<String, Comparable> o2) {
			return (ascending ? 1 : -1) * o1.get(key).compareTo(o2.get(key));
		}
	}
}