package net.sf.jabref;

import java.util.Comparator;

/**
 * 
 * A comparator for BibtexEntry fields
 * 
 * Initial Version:
 * 
 * @author alver
 * @version Date: Oct 13, 2005 Time: 10:10:04 PM To
 * 
 * Current Version:
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 * 
 * TODO: Testcases
 * 
 */
public class FieldComparator implements Comparator {

	String field;

	boolean isNameField, isTypeHeader, isYearField, isMonthField;

	int multiplier;

	public FieldComparator(String field) {
		this(field, false);
	}

	public FieldComparator(String field, boolean reversed) {
		this.field = field;
		multiplier = reversed ? -1 : 1;
		isTypeHeader = field.equals(GUIGlobals.TYPE_HEADER);

		isNameField = (field.equals("author") || field.equals("editor"));
		isYearField = field.equals("year");
		isMonthField = field.equals("month");
	}

	public int compare(Object o1, Object o2) {
		BibtexEntry e1 = (BibtexEntry) o1, e2 = (BibtexEntry) o2;

		Object f1, f2;

		if (isTypeHeader) {
			// Sort by type.
			f1 = e1.getType().getName();
			f2 = e2.getType().getName();
		} else {

			// If the field is author or editor, we rearrange names so they are
			// sorted according to last name.
			f1 = e1.getField(field);
			f2 = e2.getField(field);
		}

		// Catch all cases involving null:
		if (f1 == null)
			return f2 == null ? 0 : multiplier;

		if (f2 == null)
			return -multiplier;

		// Now we now that both f1 and f2 are != null
		if (isNameField) {
			if (f1 != null)
				f1 = AuthorList.fixAuthorForAlphabetization((String) f1);
			if (f2 != null)
				f2 = AuthorList.fixAuthorForAlphabetization((String) f2);
		} else if (isYearField) {
			/*
			 * [ 1285977 ] Impossible to properly sort a numeric field
			 * 
			 * http://sourceforge.net/tracker/index.php?func=detail&aid=1285977&group_id=92314&atid=600307
			 */
			f1 = Util.toFourDigitYear((String) f1);
			f2 = Util.toFourDigitYear((String) f2);
		} else if (isMonthField) {
			/*
			 * [ 1535044 ] Month sorting
			 * 
			 * http://sourceforge.net/tracker/index.php?func=detail&aid=1535044&group_id=92314&atid=600306
			 */
			f1 = new Integer(Util.getMonthNumber((String)f1));			
			f2 = new Integer(Util.getMonthNumber((String)f2));
			// Somehow this is twisted
			multiplier = -multiplier;
		}

		int result = 0;
		if ((f1 instanceof Integer) && (f2 instanceof Integer)) {
			result = -(((Integer) f1).compareTo((Integer) f2));
		} else if (f2 instanceof Integer) {
			Integer f1AsInteger = new Integer(f1.toString());
			result = -((f1AsInteger).compareTo((Integer) f2));
		} else if (f1 instanceof Integer) {
			Integer f2AsInteger = new Integer(f2.toString());
			result = -(((Integer) f1).compareTo(f2AsInteger));
		} else {
			String ours = ((String) f1).toLowerCase(), theirs = ((String) f2).toLowerCase();
			result = ours.compareTo(theirs);
		}

		return result * multiplier;
	}

	/**
	 * Returns the field this Comparator compares by.
	 * 
	 * @return The field name.
	 */
	public String getFieldName() {
		return field;
	}
}
