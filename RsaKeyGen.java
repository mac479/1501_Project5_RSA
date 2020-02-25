import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Random;

public class RsaKeyGen {

	private final static byte[] ONE = { (byte) 1 };

	public static void main(String[] args) throws Exception {
		Random rng = new Random(); // This seed is here for test purposes only.
		LargeInteger p;
		LargeInteger q;

		LargeInteger n; // Public key 1
		LargeInteger pn;
		LargeInteger e;
		LargeInteger[] data;
		byte[] temp = { 0x02 };

		do {
            p = new LargeInteger(256, rng);
            q = new LargeInteger(256, rng);
            n = p.multiply(q);
            pn= (p.subtract(new LargeInteger(ONE)).multiply(q.subtract(new LargeInteger(ONE))));
			e = new LargeInteger(512, rng);
			data = pn.XGCD(e);

		} while (pn.subtract(e).isNegative() || !pn.XGCD(e)[0].subtract(new LargeInteger(temp)).isNegative());
		
		if (data[2].isNegative())
			data[2] = pn.add(data[2]);
		LargeInteger d = data[2];

		File pubKey = new File("pubkey.rsa");
		File privKey = new File("privkey.rsa");
		FileOutputStream fos = new FileOutputStream(pubKey);
		fos.write((byte[]) Arrays.copyOfRange(e.getVal(), e.length() - 64, e.length()));
		fos.write((byte[]) Arrays.copyOfRange(n.getVal(), n.length() - 64, n.length()));
		fos.close();

		fos = new FileOutputStream(privKey);
		fos.write((byte[]) Arrays.copyOfRange(d.getVal(), d.length() - 64, d.length()));
		fos.write((byte[]) Arrays.copyOfRange(n.getVal(), n.length() - 64, n.length()));
		fos.close();
		
		System.out.println("Public and private keys created.");

	}

}
