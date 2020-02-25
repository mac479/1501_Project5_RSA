import java.util.Random;
import java.math.BigInteger;

public class LargeInteger {

	private final byte[] ONE = { (byte) 1 };

	private byte[] val;

	/**
	 * Construct the LargeInteger from a given byte array
	 * 
	 * @param b the byte array that this LargeInteger should represent
	 */
	public LargeInteger(byte[] b) {
		val = b;
	}

	/**
	 * Construct the LargeInteger by generatin a random n-bit number that is
	 * probably prime (2^-100 chance of being composite).
	 * 
	 * @param n   the bitlength of the requested integer
	 * @param rnd instance of java.util.Random to use in prime generation
	 */
	public LargeInteger(int n, Random rnd) {
		val = BigInteger.probablePrime(n, rnd).toByteArray();
	}

	/**
	 * Return this LargeInteger's val
	 * 
	 * @return val
	 */
	public byte[] getVal() {
		return val;
	}

	/**
	 * Return the number of bytes in val
	 * 
	 * @return length of the val byte array
	 */
	public int length() {
		return val.length;
	}

	/**
	 * Add a new byte as the most significant in this
	 * 
	 * @param extension the byte to place as most significant
	 */
	public void extend(byte extension) {
		byte[] newv = new byte[val.length + 1];
		newv[0] = extension;
		for (int i = 0; i < val.length; i++) {
			newv[i + 1] = val[i];
		}
		val = newv;
	}

	/**
	 * If this is negative, most significant bit will be 1 meaning most significant
	 * byte will be a negative signed number
	 * 
	 * @return true if this is negative, false if positive
	 */
	public boolean isNegative() {
		return (val[0] < 0);
	}

	/**
	 * Computes the sum of this and other
	 * 
	 * @param other the other LargeInteger to sum with this
	 */
	public LargeInteger add(LargeInteger other) {
		byte[] a, b;
		// If operands are of different sizes, put larger first ...
		if (val.length < other.length()) {
			a = other.getVal();
			b = val;
		} else {
			a = val;
			b = other.getVal();
		}

		// ... and normalize size for convenience
		if (b.length < a.length) {
			int diff = a.length - b.length;

			byte pad = (byte) 0;
			if (b[0] < 0) {
				pad = (byte) 0xFF;
			}

			byte[] newb = new byte[a.length];
			for (int i = 0; i < diff; i++) {
				newb[i] = pad;
			}

			for (int i = 0; i < b.length; i++) {
				newb[i + diff] = b[i];
			}

			b = newb;
		}

		// Actually compute the add
		int carry = 0;
		byte[] res = new byte[a.length];
		for (int i = a.length - 1; i >= 0; i--) {
			// Be sure to bitmask so that cast of negative bytes does not
			// introduce spurious 1 bits into result of cast
			carry = ((int) a[i] & 0xFF) + ((int) b[i] & 0xFF) + carry;

			// Assign to next byte
			res[i] = (byte) (carry & 0xFF);

			// Carry remainder over to next byte (always want to shift in 0s)
			carry = carry >>> 8;
		}

		LargeInteger res_li = new LargeInteger(res);

		// If both operands are positive, magnitude could increase as a result
		// of addition
		if (!this.isNegative() && !other.isNegative()) {
			// If we have either a leftover carry value or we used the last
			// bit in the most significant byte, we need to extend the result
			if (res_li.isNegative()) {
				res_li.extend((byte) carry);
			}
		}
		// Magnitude could also increase if both operands are negative
		else if (this.isNegative() && other.isNegative()) {
			if (!res_li.isNegative()) {
				res_li.extend((byte) 0xFF);
			}
		}

		// Note that result will always be the same size as biggest input
		// (e.g., -127 + 128 will use 2 bytes to store the result value 1)
		return res_li;
	}

	/**
	 * Negate val using two's complement representation
	 * 
	 * @return negation of this
	 */
	public LargeInteger negate() {
		byte[] neg = new byte[val.length];
		int offset = 0;

		// Check to ensure we can represent negation in same length
		// (e.g., -128 can be represented in 8 bits using two's
		// complement, +128 requires 9)
		if (val[0] == (byte) 0x80) { // 0x80 is 10000000
			boolean needs_ex = true;
			for (int i = 1; i < val.length; i++) {
				if (val[i] != (byte) 0) {
					needs_ex = false;
					break;
				}
			}
			// if first byte is 0x80 and all others are 0, must extend
			if (needs_ex) {
				neg = new byte[val.length + 1];
				neg[0] = (byte) 0;
				offset = 1;
			}
		}

		// flip all bits
		for (int i = 0; i < val.length; i++) {
			neg[i + offset] = (byte) ~val[i];
		}

		LargeInteger neg_li = new LargeInteger(neg);

		// add 1 to complete two's complement negation
		return neg_li.add(new LargeInteger(ONE));
	}

	/**
	 * Implement subtraction as simply negation and addition
	 * 
	 * @param other LargeInteger to subtract from this
	 * @return difference of this and other
	 */
	public LargeInteger subtract(LargeInteger other) {
		return this.add(other.negate());
	}

	/**
	 * Compute the product of this and other
	 * 
	 * @param other LargeInteger to multiply by this
	 * @return product of this and other
	 */
	public LargeInteger multiply(LargeInteger other) {
		byte[] mult = new byte[this.length() + other.length()];

		// Check whether the result should be positive or negative.
		byte[] a = null, b = null;
		boolean nFlag = false;
		if (this.isNegative() && other.isNegative()) {
			a = this.negate().getVal();
			b = other.negate().getVal();
		} else if (this.isNegative() || other.isNegative()) {
			// This means one is positive and other is negative.

			if (this.isNegative())
				a = this.negate().getVal();
			else
				a = this.getVal();

			if (other.isNegative())
				b = other.negate().getVal();
			else
				b = other.getVal();

			nFlag = true;
		} else {
			// Two positives.
			a = this.getVal();
			b = other.getVal();
		}

		byte[] temp = { 0 };
		LargeInteger ret = new LargeInteger(temp);
		int carryOver = 0;
		for (int i = other.length() - 1; i >= 0; i--) {
			for (int j = this.length() - 1; j >= 0; j--) {
				int x = a[j];
				if (x < 0)
					x += 256;
				int y = b[i];
				if (y < 0)
					y += 256;

				int result = (x * y) + carryOver;
				carryOver = result >> 8;
				mult[j + i + 1] = (byte) (result & 0xFF);
			}
			if (carryOver != 0) {
				mult[i] = (byte) (carryOver & 0xFF);
				carryOver = 0;
			}
			ret = ret.add(new LargeInteger(mult));
			for (int k = 0; k < mult.length; k++)
				mult[k] = 0x0;
		}

		if (nFlag)
			return ret.negate();
		return ret;
	}

	/**
	 * Run the extended Euclidean algorithm on this and other
	 * 
	 * @param other another LargeInteger
	 * @return an array structured as follows: 0: the GCD of this and other 1: a
	 *         valid x value 2: a valid y value such that this * x + other * y ==
	 *         GCD in index 0
	 */
	public LargeInteger[] XGCD(LargeInteger other) {
		LargeInteger a, b;
		if (this.isNegative())
			a = this.negate();
		else
			a = this;
		if (other.isNegative())
			b = other.negate();
		else
			b = other;

		// Make a > b
		if (a.subtract(b).isNegative())
			return b.XGCD(a);

		byte[] ZERO = { 0x00 };
		LargeInteger x = new LargeInteger(ONE), y = new LargeInteger(ZERO);
		LargeInteger x1 = new LargeInteger(ZERO), y1 = new LargeInteger(ONE);

		LargeInteger mod = a.subtract(b);
		LargeInteger q = new LargeInteger(ZERO);
		while (!mod.isNegative()) {
			q = q.add(new LargeInteger(ONE));
			a = mod;
			mod = a.subtract(b);
		}

		return XGCD(b, a, x, y, x1, y1, q);
	}

	private LargeInteger[] XGCD(LargeInteger a, LargeInteger b, LargeInteger x, LargeInteger y, LargeInteger x1,
			LargeInteger y1, LargeInteger q) {
		if (b.subtract(new LargeInteger(ONE)).isNegative()) { // if (a == 0)
			LargeInteger[] ret = new LargeInteger[3];
			ret[0] = a;
			ret[1] = x1;
			ret[2] = y1;
			return ret;
		}

		LargeInteger tempX = x, tempY = y;
		x = x1;
		y = y1;
		x1 = tempX.subtract(q.multiply(x1));
		y1 = tempY.subtract(q.multiply(y1));

		byte[] ZERO = { 0x00 };
		LargeInteger mod = a.subtract(b);
		q = new LargeInteger(ZERO);
		while (!mod.isNegative()) {
			q = q.add(new LargeInteger(ONE));
			a = mod;
			mod = a.subtract(b);
		}

		return XGCD(b, a, x, y, x1, y1, q);
	}

	/**
	 * Compute the result of raising this to the power of y mod n
	 * 
	 * @param y exponent to raise this to
	 * @param n modulus value to use
	 * @return this^y mod n
	 */
	public LargeInteger modularExp(LargeInteger y, LargeInteger n) {
		LargeInteger ret = new LargeInteger(ONE);
		LargeInteger v = this.mod(n);
		while (!y.isNegative() && !y.subtract(new LargeInteger(ONE)).isNegative()) {
			if ((y.getVal()[y.length() - 1] & 1) != 0) {
				ret = (ret.multiply(v)).mod(n);
			}
			y = y.shiftRight();
			v = (v.multiply(v)).mod(n);
		}
		return ret;

		/*
		 * LargeInteger e = new LargeInteger(ONE); if (y.isNegative()) e.subtract(new
		 * LargeInteger(ONE)); else { for (int i = 0; i < y.length(); i++) { for (int j
		 * = 1; j < 8; j <<= 1) { System.out.println(i+" "+j+" "+e.length()); e =
		 * e.multiply(e); if ((y.getVal()[i] & j) != 0) e = e.multiply(this); } } }
		 * LargeInteger mod = e.subtract(n); while (!mod.isNegative()) { e = mod; mod =
		 * e.subtract(n); } return e;
		 */
	}

	private LargeInteger mod(LargeInteger other) {
		byte[] ZERO = { 0x00 };
		LargeInteger q = new LargeInteger(ZERO);
		LargeInteger a;
		LargeInteger b;

		if (this.isNegative())
			a = this.negate();
		else
			a = this;
		if (other.isNegative())
			b = other.negate();
		else
			b = other;

		int shift = 0;
		while (!a.subtract(b).isNegative()) {
			b = b.shiftLeft();
			shift++;
		}
		b = b.shiftRight();

		while (shift > 0) {
			q = q.shiftLeft();
			if (!(a.subtract(b).isNegative())) {
				a = a.subtract(b);
				q = q.add(new LargeInteger(ONE));
			}
			b = b.shiftRight();
			shift--;
		}

		if (this.isNegative() == other.isNegative())
			return this.clean(this.subtract(other.multiply(q)));
		else
			return this.clean(this.subtract(other.multiply(q.negate())));
	}

	// A recursive function to clean up values from functions.
	private LargeInteger clean(LargeInteger other) {
		if (other.length() > 1) {
			if (((other.getVal()[0] == 0) && ((other.getVal()[1] & 0x80) == 0x00))
					|| ((other.getVal()[0] & 0xff) == 0xff && (other.getVal()[1] & 0x80) == 0x80)) {
				
				byte[] newVal = new byte[other.length() - 1];
				for (int i = 0; i < newVal.length; i++)
					newVal[i] = other.val[i+1];
				
				return clean(new LargeInteger(newVal));
			}
		}

		return other;
	}

	private LargeInteger shiftRight() {
		byte[] shift;
		int i = 0;
		if (this.getVal()[0] == 0 && (this.getVal()[1] & 0x80) != 0) {
			shift = new byte[this.length() - 1];
			i++;
		} else
			shift = new byte[this.length()];
		int m, m1; // m1 is the previous m. Used to handle sign.
		if (this.isNegative())
			m1 = 1;
		else
			m1 = 0;

		for (int j = 0; j < shift.length; j++, i++) {
			m = this.getVal()[i] & 0x01;
			shift[j] = (byte) ((this.getVal()[i] >> 1) & 0x7f);
			shift[j] |= m1 << 7;
			m1 = m;
		}
		return new LargeInteger(shift);
	}

	private LargeInteger shiftLeft() {
		byte[] shift;
		if ((this.getVal()[0] & 0x40) != 0 && !this.isNegative())
			shift = new byte[this.length() + 1];
		else
			shift = new byte[this.length()];

		int m, m1 = 0;
		for (int i = 1; i <= this.length(); i++) {
			m = (this.getVal()[this.length() - i] & 0x80) >> 7;
			shift[shift.length - i] = (byte) (this.getVal()[this.length() - i] << 1);
			shift[shift.length - i] |= m1;
			m1 = m;
		}
		return new LargeInteger(shift);
	}
}
