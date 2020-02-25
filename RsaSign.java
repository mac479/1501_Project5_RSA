import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;

public class RsaSign {

	public static void main(String[] args) throws Exception {
		
		Path path = Paths.get(args[1]);
		if(!path.toFile().exists()) {
			System.out.println("ERROR: Cannot find file specified!");
			return;
		}
		byte[] data = Files.readAllBytes(path);
		
		if (args[0].equalsIgnoreCase("s")) {
			
			File privKey = new File("privkey.rsa");
			if(!privKey.exists()) {
				System.out.println("ERROR: privkey.rsa not found!");
				return;
			}
			FileInputStream fis=new FileInputStream(privKey);
			byte[] tempD=new byte[65];
			byte[] tempN=new byte[65];
			fis.read(tempD,1,64);
			fis.read(tempN,1,64);
			fis.close();
			LargeInteger d=new LargeInteger(tempD);
			LargeInteger n=new LargeInteger(tempN);
			
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(data);
			byte[] digest = md.digest();
			
			LargeInteger hash=new LargeInteger(digest);
			LargeInteger sHash=hash.modularExp(d, n);

			File sigFile = new File(args[1]+".sig");
			FileOutputStream fos = new FileOutputStream(sigFile);
			fos.write(sHash.getVal());
			fos.close();
			System.out.println("File sucessfully signed.");
		}
		if(args[0].equalsIgnoreCase("v")) {
			File sigData=new File(args[1]+".sig");
			if(!sigData.exists()) {
				System.out.println("ERROR: .sig file for"+args[1]+" not found!");
				return;
			}
			FileInputStream fis2=new FileInputStream(sigData);
			byte[] binDat=fis2.readAllBytes();
			LargeInteger sig=new LargeInteger(binDat);
			fis2.close();
			
			File pubKey=new File("pubkey.rsa");
			if(!pubKey.exists()) {
				System.out.println("ERROR: pubkey.rsa not found!");
				return;
			}
			fis2=new FileInputStream(pubKey);
			byte[] tempE=new byte[65];
			byte[] tempN=new byte[65];
			fis2.read(tempE,1,64);
			fis2.read(tempN,1,64);
			fis2.close();
			LargeInteger e=new LargeInteger(tempE);
			LargeInteger n=new LargeInteger(tempN);
			
			sig=sig.modularExp(e, n);

			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(data);
			byte[] digest = md.digest();
			LargeInteger hash=new LargeInteger(digest);
			
			if(checkEquality(hash,sig)) 
				System.out.println("The signature was verified sucessfully.");
			else
				System.out.println("The signature failed to be verified.");
			
		}
	}

	private static boolean checkEquality(LargeInteger hash, LargeInteger sig) {
		if(hash.length()!=sig.length())
			return false;
		for(int i=0;i<hash.length();i++) {
			if(hash.getVal()[i]!=sig.getVal()[i])
				return false;
		}
		return true;
	}

}
