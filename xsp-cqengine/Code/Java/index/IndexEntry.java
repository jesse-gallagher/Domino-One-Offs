package index;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.googlecode.cqengine.attribute.SimpleAttribute;
import com.ibm.xsp.model.DataObject;

public class IndexEntry implements Serializable, Comparable<IndexEntry>, DataObject {
	private static final long serialVersionUID = 1L;

	private final String documentId_;
	private final Map<String, Object> attributes_ = new HashMap<String, Object>();

	public IndexEntry(final String documentId, final Map<String, Object> values) {
		documentId_ = documentId;
		attributes_.putAll(values);
	}

	public Object getAttribute(final String key) {
		return attributes_.get(key);
	}

	public String getDocumentId() {
		return documentId_;
	}

	public int compareTo(final IndexEntry o) {
		return documentId_.compareTo(o.getDocumentId());
	}

	@Override
	public int hashCode() {
		return getClass().getName().hashCode() + documentId_.hashCode();
	}

	@Override
	public String toString() {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("documentId", documentId_);
		result.put("attributes", attributes_);
		return result.toString();
	}

	public Class<Object> getType(final Object key) {
		return Object.class;
	}

	public Object getValue(final Object key) {
		if ("@documentId".equals(key)) {
			return getDocumentId();
		}
		return getAttribute(String.valueOf(key));
	}

	public boolean isReadOnly(final Object key) {
		return true;
	}

	public void setValue(final Object key, final Object value) {}

	public static final SimpleAttribute<IndexEntry, String> DOCUMENTID_ATTRIBUTE = new SimpleAttribute<IndexEntry, String>() {
		@Override
		public String getValue(final IndexEntry entry) {
			return entry.getDocumentId();
		}
	};

	public static SimpleAttribute<IndexEntry, String> attr(final String key) {
		return new SimpleAttribute<IndexEntry, String>(key) {
			@Override
			public String getValue(final IndexEntry entry) {
				return String.valueOf(entry.getAttribute(key));
			}
		};
	}
}
