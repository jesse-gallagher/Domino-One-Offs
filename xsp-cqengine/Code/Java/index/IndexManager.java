package index;

import java.io.Serializable;
import java.util.*;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.googlecode.cqengine.CQEngine;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.index.navigable.NavigableIndex;
import com.googlecode.cqengine.index.unique.UniqueIndex;
import com.ibm.xsp.model.DataObject;

import frostillicus.xsp.bean.ApplicationScoped;
import frostillicus.xsp.bean.ManagedBean;
import frostillicus.xsp.util.FrameworkUtils;

import org.openntf.domino.*;

/**
 * For now, this class just reads an existing vidw index. In a real situation, it should be paired with
 * an alternative indexer that builds the Multiset (or just a Set) itself and then provides that result
 * to this class. This will do for a proof-of-concept, though.
 * 
 * @author jesse
 *
 */
@ManagedBean(name = "Indexes")
@ApplicationScoped
public class IndexManager implements Serializable, DataObject {
	private static final long serialVersionUID = 1L;

	private final Map<String, IndexedCollection<IndexEntry>> cache_ = new HashMap<String, IndexedCollection<IndexEntry>>();

	public static IndexManager get() {
		return (IndexManager) FrameworkUtils.resolveVariable(IndexManager.class.getAnnotation(ManagedBean.class).name());
	}

	@SuppressWarnings("unchecked")
	public Class<IndexedCollection> getType(final Object key) {
		return IndexedCollection.class;
	}

	public IndexedCollection<IndexEntry> getValue(final Object key) {
		if (!(key instanceof String)) {
			throw new IllegalArgumentException("key must be a String");
		}

		if (!cache_.containsKey(key)) {
			String stringKey = (String) key;
			Database database = FrameworkUtils.getDatabase();


			// Since we're basing this on existing views, iterate over the view's entries
			// to build a Multiset
			View view = database.getView(stringKey);
			view.setAutoUpdate(false);
			Multiset<IndexEntry> table = HashMultiset.create();

			ViewNavigator nav = view.createViewNav();
			nav.setBufferMaxEntries(400);
			for (ViewEntry entry : nav) {
				table.add(new IndexEntry(entry.getUniversalID(), entry.getColumnValuesMap()));
			}

			IndexedCollection<IndexEntry> index = CQEngine.copyFrom(table);

			// For now, add indexes for all columns (which will be returned as Strings)
			index.addIndex(UniqueIndex.onAttribute(IndexEntry.DOCUMENTID_ATTRIBUTE));
			for (ViewColumn col : view.getColumns()) {
				index.addIndex(NavigableIndex.onAttribute(IndexEntry.attr(col.getItemName())));
			}

			cache_.put(stringKey, index);
		}
		return cache_.get(key);
	}

	public boolean isReadOnly(final Object key) {
		return true;
	}

	public void setValue(final Object key, final Object value) {}

}
