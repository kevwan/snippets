package samples.consistenthash;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Hash implements HashFunction
{
	@Override
	public int hash(Object code)
	{
		try
		{
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(code.toString().getBytes());
			byte[] data = md.digest();
			return new BigInteger(data).intValue();
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		return 0;
	}
}
