package utils;

import java.io.Serializable;

public class Tuple<X, Y> implements Serializable {
	public final X x;
	public final Y y;

	public Tuple(X x, Y y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Tuple<?, ?> tuple = (Tuple<?, ?>) o;
		if (!x.equals(tuple.x))
			return false;
		return y.equals(tuple.y);
	}

	@Override
	public int hashCode() {
		int result = x.hashCode();
		result = 31 * result + y.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "(" + x.toString().replaceAll("!val!", "") + "," + y.toString().replaceAll("!val!", "") + ")";
	}

}