package samples.consistenthash;

import java.util.Arrays;
import java.util.List;

public class Client
{
	public static void main(String[] args)
	{
		List<String> nodes = Arrays.asList("redis1", "redis2", "redis3", "redis4");
		String[] userIds = { "-84942321036308", "-76029520310209", "-68343931116147", "-54921760962352", "12342134134" };

		ConsistentHash<String> consistentHash = new ConsistentHash<String>(new MD5Hash(), 100, nodes);
		for (String userId : userIds)
		{
			System.out.println(userId + " on " + consistentHash.get(userId));
		}
	}
}
