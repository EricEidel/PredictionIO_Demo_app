/**
 * This is a thin wrapper with some debugging capabilities around the client class. 
 * It's main purpose is to abstract the layers under the hood and only expose one unified class as an API to talk to the engine.
 * This does not cover all cases - sometimes, you would have to go deeper in. 
 * 
 * However, I hope this will be useful to someone who just wants to connect to a prediction IO server.
 * 
 * @author Eric Eidelberg
 * @email ninuson123@gmail.com
 */

package wrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import prediction.Client;
import prediction.CreateItemRequestBuilder;
import prediction.FutureAPIResponse;
import prediction.Item;
import prediction.ItemRecGetTopNRequestBuilder;
import prediction.ItemSimGetTopNRequestBuilder;
import prediction.User;
import prediction.UserActionItemRequestBuilder;

public class PredIOConn 
{
	private boolean debug = false;
	private Client client;
	public String appkey;
	
	List<FutureAPIResponse> rs = new ArrayList<FutureAPIResponse>();
	List<DebugResponeWithString> rs_debug = new ArrayList<DebugResponeWithString>();
	/**
	 * This is the connection class. It takes care of connecting to the engine and manages any interaction with it.
	 * 
	 * @param appurl - the url for the engine. Defualt is "http://localhost:8000"
	 * @param appkey - this is the app key from the admin panel.
	 */
	public PredIOConn(String appurl, String appkey)
	{        
        client = new Client(appkey,appurl);
        this.appkey = appkey;
        
        // Get API system status if debugging
        if (debug)
        {
        	try 
	        {
	            DEBUG(client.getStatus());
	        } 
	        catch (Exception e) 
	        {
	        	DEBUG("Unable to get status: "+e.getMessage());
	        }
        }
	}
	
	/**
	 * This is the connection class. It takes care of conneting to the engine and manages any interaction with it.
	 * Uses the default is "http://localhost:8000".
	 * 
	 * 
	 * @param appkey - this is the app key from the admin panel.
	 */
	public PredIOConn(String appkey)
	{
		this("http://localhost:8000", appkey);
	}
	
	/**
	 * This is a debug utility to know what the results were.
	 */
	public void debug_results()
	{
		for (DebugResponeWithString dr : rs_debug) 
			DEBUG(dr.toString());
	}

	
	/**
	 * Gets the status of the engine
	 * @return the status of teh engine as a string
	 * @throws IOException 
	 * @throws InterruptedException 
	 * @throws ExecutionException 
	 */
	public String getStatus()
	{
		try 
		{
			return client.getStatus();
		} 
		catch (Exception e) 
		{
			return e.getMessage();
		} 
	}
	
	/**
	 * This method tries to get a single item from the engine.
	 * 
	 * @param item_id the name of the item in the engine.
	 */
	public Item getItem(String item_id)
	{
		try
		{
			Item item = client.getItem(item_id);
			return item;
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	/**
	 * This method tries to add a single user to the engine.
	 * 
	 * @param user_id the name of the user in the engine. In case the user already exists in the engine, it will be silently over-ridden!
	 */
	public void addUser(String user_id) 
	{
		DEBUG_SNG("Create dummy user with User ID: " + user_id + " ... " );
        try 
        {
            //rs.add(client.createUserAsFuture(client.getCreateUserRequestBuilder(user_id)));
            rs_debug.add(new DebugResponeWithString(client.createUserAsFuture(client.getCreateUserRequestBuilder(user_id)), user_id));
            DEBUG(" sent!");
        } 
        catch (Exception e) 
        {
        	DEBUG("Could not create user " + user_id);
            e.printStackTrace();
        }
	}
	
	/**
	 * This method tries to add a single item to the engine.
	 * 
	 * @param item_id the name of the item in the engine. In case the user already exists in the engine, it will be silently over-ridden!
	 * @param itpes an array of strings, each string being a type associated with the item.
	 */
	public void addItem(String item_id, String[] itypes) 
	{
        DEBUG_SNG("Add dummy item with item ID: " + item_id + " ... " );

        try 
        {
            rs_debug.add(new DebugResponeWithString(client.createItemAsFuture(client.getCreateItemRequestBuilder(item_id, itypes)), 
            		(item_id)));
            DEBUG(" sent!");
        } 
        catch (Exception e) 
        {
        	DEBUG("Could not create item " + item_id);
            e.printStackTrace();
        }
	}


	/**
	 * This method tries to add a single item to the engine.
	 * 
	 * @param item_id the name of the item in the engine. In case the item already exists in the engine, it will be silently over-ridden!
	 * @param itpes an array of strings, each string being a type associated with the item.
	 * @param attributes - optional attributes that will be appended to the item.
	 */
	public void addItem(String item_id, String[] itypes, ArrayList<Attribute> attributes) 
	{
		DEBUG_SNG("Add dummy item with item ID: " + item_id + " ... " );

        try 
        {
        	CreateItemRequestBuilder builder = client.getCreateItemRequestBuilder(item_id, itypes);
        	for (Attribute a: attributes)
        	{
        		builder = builder.attribute(a.getName(), a.getValue());
        	}
        	
            rs_debug.add(new DebugResponeWithString(client.createItemAsFuture(builder), 
            		(item_id + " and some attributes.")));

            DEBUG(" sent!");
        } 
        catch (Exception e) 
        {
        	DEBUG("Could not create item " + item_id);
            e.printStackTrace();
        }
	}
	
	/**
	 * Identify the actions to a certain user
	 * 
	 * @param user_id - the user to identify as.
	 */
	public void identify(String user_id)
	{
		client.identify(user_id);
	}
	
	/**
	 * Identify the actions to a certain user
	 * 
	 * @param user_id - the user to identify as.
	 * @return user - the user that the user_id belongs to.
	 */
	public User getUser(String user_id)
	{
		try 
		{
			User user = client.getUser(user_id);
			return user;
		} 
		catch (Exception e) 
		{
			return null;
		} 
	}
	
	/**
	 * This method sends a like event to the engine.
	 * 
	 * @param user_id - the id of the user who's performing the action.
	 * @param item_id - the id of the item on which the action is performed.
	 */
	public void like(String user_id, String item_id) 
	{
		DEBUG_SNG("Performing a like: " + user_id + " " + item_id + " ... " );
		client.identify(user_id);
		try 
		{
			client.userActionItem("like", "the driver id");
			DEBUG("sent!");
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	
	/**
	 * This method sends a dislike event to the engine.
	 * 
	 * @param user_id - the id of the user who's performing the action.
	 * @param item_id - the id of the item on which the action is performed.
	 */
	public void dislike(String user_id, String item_id) 
	{
		DEBUG_SNG("Performing a dislike: " + user_id + " " + item_id + " ... " );
		client.identify(user_id);
		try 
		{
			client.userActionItem("dislike", "the driver id");
			DEBUG("sent!");
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * This method sends a rate event to the engine.
	 * 
	 * @param user_id - the id of the user who's performing the rating.
	 * @param item_id - the id of the item that is being rated.
	 * @param rating - the rating, has to be an integer from 1 to 5. (inclusinve)
	 */
	public void rate(String user_id, String item_id, int rating) 
	{
		DEBUG_SNG("User " + user_id + " rated " + item_id + " with rating " +  rating + " ... " );
		client.identify(user_id);
		try 
		{
			UserActionItemRequestBuilder builder = client.getUserActionItemRequestBuilder("rate", item_id)
				    .rate(rating);
			client.userActionItem(builder);
			DEBUG("sent!");
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Get similar items to the item given.
	 * 
	 * @param user_id - the user for which the recommendation is needed.
	 * @param engine - the name of the engine on the app that will generate the recommendation.
	 * @param num - the number of recommended items.
	 * 
	 * @return a string array with the item id's. In case no recommendation is made, returns null.
	 */
	public String[] getSim(String item_id, String[] itypes, String engine, int num) 
	{
		String[] recommendations;
		try 
		{
			ItemSimGetTopNRequestBuilder builder = client.getItemSimGetTopNRequestBuilder("engine", item_id, 5)
					  .itypes(itypes);
			recommendations = client.getItemSimTopN(builder);
			DEBUG_ARR(recommendations);
			return recommendations;
		} 
		catch (Exception e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

	/**
	 * Get similar items to the item given.
	 * 
	 * @param user_id - the user for which the recommendation is needed.
	 * @param engine - the name of the engine on the app that will generate the recommendation.
	 * @param num - the number of recommended items.
	 * 
	 * @return a string array with the item id's. In case no recommendation is made, returns null.
	 */
	public String[] getRec(String user_id, String engine, int num) 
	{
		String[] recommendations;
		client.identify(user_id);
		
		try 
		{
			ItemRecGetTopNRequestBuilder builder = client.getItemRecGetTopNRequestBuilder(engine, num);
			recommendations = client.getItemRecTopN(builder);
			
			DEBUG_ARR(recommendations);
			
			return recommendations;
		} 
		catch (Exception e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	
	public void close()
	{
		client.close();
	}
	
	// UTIL METHODS

	public void DEBUG(String str)
	{
		if (debug)
			System.out.println(str);
	}
	
	public void DEBUG_ARR(String[] str_arr)
	{
		if (debug)
		{
			System.out.println("The response was: ");
			for (String s: str_arr)
			{
				System.out.print(s + ",");
			}
			System.out.println();
		}
	}
	
	
	private void DEBUG_SNG(String str) 
	{
		if (debug)
			System.out.print(str);
	}
	
	public boolean isDebug() 
	{
		return debug;
	}

	public void setDebug(boolean debug) 
	{
		this.debug = debug;
	}

}
