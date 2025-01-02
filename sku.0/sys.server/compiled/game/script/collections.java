/* 
 Title:        collections.java
 Description:  adds functionality similar to the java.util.Collections class
 */

package script;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class collections
{
	/**
	 * Creates a new Map from two parallel arrays. The arrays must be non-null and of the same length.
	 * The data in the keys array must be non-null, but the values data may be null.
	 *
	 * @return the mapping of the keys to the values, or null on error
	 */
	public static Map newMap(Object[] keys, Object[] values)
	{
		if ( keys == null || values == null || keys.length != values.length )
			return null;
		Map map = new HashMap(keys.length);
		for ( int i = 0; i < keys.length; ++i )
		{
			if ( keys[i] == null )
				return null;
			map.put(keys[i], values[i]);
		}
		return map;
	}

	public static <T> T[] removeElement(T[] array, T element) {
		// Use an ArrayList as an intermediary
		ArrayList<T> tempList = new ArrayList<>();
		for (T item : array) {
			if (!item.equals(element)) {
				tempList.add(item);
			}
		}

		return tempList.toArray(Arrays.copyOf(array, 0));
	}

	public static <T> T[] addElement(T[] array, T element) {
		// Create a new array with one additional slot
		T[] newArray = Arrays.copyOf(array, array.length + 1);

		// Add the new element to the last position
		newArray[array.length] = element;

		return newArray;
	}
}

