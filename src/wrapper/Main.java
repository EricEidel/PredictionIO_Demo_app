/**
 * This is a test application, showing how to use the wrapper around the client class.
 * 
 * @author Eric Eidelberg
 * @email ninuson123@gmail.com
 */


package wrapper;

import java.util.ArrayList;

public class Main 
{
	static boolean VERB_DEBUG = false;
	
	public static void main (String[] args)
	{
		/* set appurl to your API server */
        String appurl = "http://localhost:8000";
        /* set appkey for the engine. */
        String appkey = "fLLom1egOXRnw1UcdBlqmQLuiHJpavZT8JZQGRxEE1mutYOFKrfbb2IyNFunPYRk";
        String appkey_2 = "ZL70ye3wxYGlfJGVX077r8R4XsB6oM60PXoqDfxbgDk3pOppnxKCj02ztOAVLrET";
        
        // Initiate the connector
		PredIOConn conn = new PredIOConn(appurl, appkey_2);
		
		// optionally - set a specific engine from the app.
		// conn.setEngine("engine_name");
		
		// Set debug console printouts on.
		conn.setDebug(true);
		
		conn.dislike("user_test_1", "item_test_1");
		// Run the features tests.
		//test(conn);
		//generate_sample_data(conn);
		//getRec(conn);
		conn.close();
	}

	// Get some recommendations.
	private static void getSim(PredIOConn conn) 
	{
		String[] itypes = {"posts"};
		
		conn.getSim("item_test_1", itypes, "engine" , 2);
	}

	// Get some recommendations.
	private static void getRec(PredIOConn conn) 
	{		
		conn.getRec("user_test_1", "tryRec" , 2);
	}
	
	// Create 100 users, 50 items. Make all users like the first item, 50 users like the 2nd item and 10 users like the 3rd.
	private static void generate_sample_data(PredIOConn conn) 
	{
		String user_id_base = "user_test_";
		String item_id_base = "item_test_";
		String[] itypes = {"posts"};

		for (int i = 1 ; i<11; i++)
		{
			conn.addUser(user_id_base + i);
		}
		
		for (int i = 1 ; i<11; i++)
		{
			conn.addItem((item_id_base+i), itypes);
		}
		
		for (int i = 1; i<11; i++)
		{
			conn.like((user_id_base+i), (item_id_base + "1"));
		}
		
		for (int i = 2; i<11; i++)
		{
			conn.like((user_id_base+i), (item_id_base + "1"));
		}
		
		for (int i = 1; i<11; i++)
		{
			conn.rate((user_id_base+i), (item_id_base + "3"), 4);
		}
		
		conn.close();
		System.out.println("Done!");
	}

	private static void test(PredIOConn conn) 
	{
		// Hard coded test names. TODO file parser
		String user_id = "foobar1";
		String user_id2 = "foobar2";
		String item_id = "item2";
		String item_id2 = "item2_with_att";
        String[] itypes = {"posts"};
        
        // The attribute list is an optional parameter you can an item in PredictionIO. Each item can have none, one or many attributes. 
        // These attributes have a name and a value
        ArrayList<Attribute> attributes = new ArrayList<Attribute>();
        
        attributes.add(new Attribute("url","http://dummyurl.com"));
        attributes.add(new Attribute("startT","ignored"));
        
        // Add a single user
		//conn.addUser(user_id);
		//conn.addUser(user_id2);
		// Add a single item, no attributes.
		//conn.addItem(item_id, itypes);
		
		// Add a single item with a list of attributes.
		//conn.addItem(item_id2, itypes, attributes);
		
		// Perform a like action. user_id likes item_id.
		conn.like(user_id, item_id);
		conn.like(user_id2, item_id);
		
		// This will print out the result of all requests and the description string of which item that was.
		if (VERB_DEBUG)
			conn.debug_results();
		
		// Close the client. This is important for a clean shutdown.
		conn.close();
		System.out.println("Done testing!");
	}
}
