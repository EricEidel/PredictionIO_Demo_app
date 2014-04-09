package eric.eidel.redrecommend;

import java.util.ArrayList;
import java.util.List;

import mapping.RedditAccount;
import mapping.RedditLink;
import prediction.Item;
import prediction.User;
import wrapper.PredIOConn;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

/**
 * 
 * @author Arkady (Eric) Eidelberg
 * @email ninuson123@gmail.com
 * 
 * This is where the heavy lifting is preformed once a user is connected to Reddit from the main activity.
 * This page assumes a reddit instance was already instanciated and is in the singleton instance.
 *
 * Throughout the entire application there are many AsyncTask<?,?,?> classes. 
 * This was the only working solution I could find to connect to the internet.
 * 
 * A few adjustments might be needed to set this app working on another computer:
 * 1) In the RecommendeationPage activity, change the REC_ENGINE_NAME to the name of you engine.
 * 2) In the same activity, change the APPKEY_REC to the app key.
 * 
 *  After that, the app should run as normal.
 *  
 *  The recommendation engine only stores reddit API fullnames: (t3_xxxxxx) where x is a digit or a letter
 *  Once the recommendation is available, it has to be fetched from the reddit server.
 */
public class RecommendationPage extends ListActivity 
{
	// Names and such
	static final String REC_ENGINE_NAME = "tryRec";
	
	// set app keys for the engine. 
	static String APPKEY_REC = "ZL70ye3wxYGlfJGVX077r8R4XsB6oM60PXoqDfxbgDk3pOppnxKCj02ztOAVLrET";
	
	static final int TARGET_POSTS = 25;
	
	// localhost on android is 10.0.2.2
	static String APPURL = "http://10.0.2.2:8000";
	 
	// Must have been initialized before in the main activity or this page wouldn't show.
	Reddit reddit = Reddit.getInstance("");
	
	// These are instances of the wrappers for each engine.
	PredIOConn rec_engine = null;
	String username = "";
	
	// This is the list that will be updated throughout the code. These items will show in the GUI.
	ArrayList<RedditLink> items_to_display = new ArrayList<RedditLink>();
	
	// This is the list adapter. Works like ListModel in java awt, the "brains" behind the list.
	// If items_to_display changes (which it most likly does) need to call:
	// adapter.notifyDataSetChanged()
	RedditLinkAdapter adapter;
	
	// Responses from the rec_engine, internal use
	String[] recommendations_arr = null; // These come as responses from the engine
	List<RedditLink> rec_list = null; // These are obtained from reddit afterwards
	
	
	// The reddit account with which the user is connected.
	RedditAccount account;
	
	/**
	 * Entrance point to this activity.
	 * @param Bundle contains the user name from the previous activity.
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		// Set up the adapter for this list activity
		adapter = new RedditLinkAdapter(getApplicationContext(), items_to_display);
		setListAdapter(adapter);
		
		 // Get the message from the intent
		Intent intent = getIntent();
		
		// Store user name to identify to the engine
		username = intent.getStringExtra(MainActivity.USER_NAME);

		// Get the reddit account for this user
		GetAccountInfo check_account = new GetAccountInfo();
		check_account.execute();
		
		// Connect to the recommendation engine.
		// Once connected, get recomendations is called from within.
		SetupPredictionIO setup_rec_engine = new SetupPredictionIO();
		setup_rec_engine.execute();
		
	}

	/**
	 * Not used, required by android
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.recommendation_page, menu);
		return true;
	}
	
	/**
	 * Internal use. 
	 * Sets the current account to the one obtained from reddit with all the stats.
	 * Logs the answer.
	 * 
	 * @param account - the account with which the user is connected.
	 */
	public void tell_account(RedditAccount account)
	{
		this.account = account;
		Log.d("account info", account.toString());
	}
	
	
	/**
	 * Internal use. 
	 * Logs status of the engine.
	 * 
	 * @param status - the status of the engine.
	 */
	public void log_status_rec_engine(String status)
	{
		Log.d("ENGINE", "REC:"+status);
	}
		
	/**
	 * Internal use. 
	 * Adds the rec_engine to this activity and requests for initial recommendations.
	 * 
	 * @param pio - the warpper of the engine.
	 */
	public void add_rec_engine(PredIOConn pio)
	{
		rec_engine = pio;
		
		// Get the recommendations for this user
		GetRecommendation gr = new GetRecommendation();
		gr.execute();
	}
	
	/**
	 * Internal use. 
	 * Logs status of the user.
	 */
	public void user_identified()
	{
		Log.d("ENGINE", "User " + username + " was successfuly identified.");
		// User is identified, can get recs and default behaviour and such.
	}
	
	
	/**
	 * This method is evoked when there are recommendations for the user from the engine.
	 * 
	 * @param recommendations - the array of recommendations that came from the engine.
	 */
	public void got_recommendations(String[] recommendations)
	{
		// if there are some recommendations, set them to be known to the activity.
		if (recommendations != null)
		{
			Log.d("ENGINE", "Size of recommend was: "+recommendations.length);
			recommendations_arr = recommendations;
			
			// Go to reddit and get the recommendations!
			RecGrabber rg = new RecGrabber();
			rg.execute();
		}
		else
		{	// No recamendations at all, go get full target number of post from the defualt behaviour.
			Log.d("ENGINE", "Can't make a recommendation!");
			get_default_behaviour(TARGET_POSTS);
		}
	}
	
	/**
	 * This method is evoked when there are NOT ENOUGH recommendations for the user from the engine.
	 * 
	 * @param num_of_needed_default - how many top posts from the front page to grab.
	 */
	private void get_default_behaviour(int num_of_needed_default) 
	{
		GetDefault gd = new GetDefault();
		gd.execute(num_of_needed_default);
	}
	 
	/**
	 * This method is evoked when the default behavior is finished fetching posts from the front page.
	 * 
	 * @param list_defualt - list of default posts. This will be used as much as needed to meet the target.
	 */
	void display_results(List<RedditLink> list_defualt)
	{
		
		// Display recommendations first, if not empty.
		if (rec_list != null)
		{			
			items_to_display.addAll(rec_list);
		}
		
		// Find out how much more needed to meet the target
		int num_to_fill = TARGET_POSTS - items_to_display.size();
		
		// Add that many posts
		for (int i = 0; i<num_to_fill; i++)
		{
			items_to_display.add(list_defualt.get(i));
		}
		
		// Update the adapter to display new info!
		adapter.notifyDataSetChanged();
	}

	/**
	 * This is evoked when a link is clicked in the list. 
	 * Opens that link in the default browser.
	 * 
	 * @param url - the link of the RedditLink on which the user clicked.
	 */
	private void open_browser(String url) 
	{
		// Open a browser activity.
		Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		startActivity(i);
	}
	
	// **************************************************************************
	// ======================= PRIVATE CLASSES START HERE =======================
	// **************************************************************************
	
	/**
	 * @author Arkady Eidelberg
	 * 
	 * This class defines the custom ArrayAdapter for this list activity.
	 *
	 */
	private class RedditLinkAdapter extends ArrayAdapter<RedditLink> 
	{
		// needed further on for inflator
		private final Context context;
		// The array list of links
		private final ArrayList<RedditLink> values;
		
		// Constructor. Set up the super class and save needed stuff.
		public RedditLinkAdapter(Context context, ArrayList<RedditLink> values) 
		{
			super(context, R.layout.list_posts, values);
			this.context = context;
			this.values = values;
		}
	
		/**
		 * Used internally to populate the list activity and build many list_posts reletive layouts.
		 * 
		 * @param position - which item in the list is being populated now. Used as index into values.
		 * @param convertView - internally used
		 * @param parent - internally used to associate the new view with the listView of the list activity.
		 */
		@Override
		public View getView(int position, View convertView, ViewGroup parent) 
		{
			// get an inflater from the OS
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			// "parse" the xml layout list_posts to create a container view
			View rowView = inflater.inflate(R.layout.list_posts, parent, false);
			
			// Get the sub-views
			TextView textView_title = (TextView) rowView.findViewById(R.id.title);
			TextView textView_score = (TextView) rowView.findViewById(R.id.score);
			Button up = (Button) rowView.findViewById(R.id.upvote); 
			up.setText("/\\");
			Button down = (Button) rowView.findViewById(R.id.downvote);
			down.setText("\\/");
			
			// Get the related link associated with this view.
			RedditLink link = values.get(position);

			textView_title.setText(link.getTitle());
			
			// Set up the tag array, needed for the title textview, since it needs both id and url
			String[] title_tag = new String[2];
			title_tag[0] = link.getUrl();
			title_tag[1] = "t3_" + link.getId();
			textView_title.setTag(title_tag);
			
			// If user clicks on the title, let engine know he "viewed" the item and open a browser with that link
			textView_title.setOnClickListener(new OnClickListener() 
			{
				@Override
				public void onClick(View view) 
				{
					// Grab tag and it's parameters
					String[] tag_arr = (String[]) view.getTag();
					String url = tag_arr[0];
					String fullname = tag_arr[1];
					
					Log.d("ENGINE", "TEXT clicked - link: " + url);
				
					// Update engine with "visit" event
					EngineUpdater eu = new EngineUpdater();
					eu.execute(EngineUpdater.VISIT, fullname, username);
					
					// Open the link
					open_browser(url);
				}
			});
			
			// Display the score
			Integer score = Integer.valueOf(link.getUps() - link.getDowns());		
			textView_score.setText(score.toString());
			
			// Display the up button
			up.setTag("t3_"+link.getId());
			
			// If user clicks the up button, send a "up vote" event to the engine and update reddit
			up.setOnClickListener(new OnClickListener() 
			{
				@Override
				public void onClick(View view) 
				{
					String fullname = (String) view.getTag();
					Log.d("ENGINE", "UP clicked " + fullname);
					
					// Update reddit
					Voter voter = new Voter();
					voter.execute(Voter.UP_VOTE, fullname);
					
					// Update engine
					EngineUpdater eu = new EngineUpdater();
					eu.execute(EngineUpdater.UP_VOTE, fullname, username);
				}
			});
			
			// display the down button
			down.setTag("t3_"+link.getId());
			
			// send a down vote event to the engine on click and update reddit
			down.setOnClickListener(new OnClickListener() 
			{
				@Override
				public void onClick(View view) 
				{
					String fullname = (String) view.getTag();
					Log.d("ENGINE", "DOWN clicked " + fullname);
					
					// Update reddit
					Voter voter = new Voter();
					voter.execute(Voter.DOWN_VOTE, fullname);
					
					// Update engine
					EngineUpdater eu = new EngineUpdater();
					eu.execute(EngineUpdater.DOWN_VOTE, fullname, username);
				}
			});
			
			rowView.setTag(link);
			
			return rowView;
		}
	}
	
	// **************************************************************************
	// *		All these classes are needed for easy network access.           *
	// **************************************************************************
	
	/**
	 * @author Arkady Eidelberg
	 * 
	 * This class gets the user's account info from reddit.
	 *
	 */
	private class GetAccountInfo extends AsyncTask<String, Void, RedditAccount> 
	{
		/** 
		 * This method runs when execture() is preformed.
		 * It takes no parameters, since reddit singleton is already instanciated.
		 */
		protected RedditAccount doInBackground(String... params) 
		{
			try 
			{
				RedditAccount account = reddit.meJson();

				return account;
			} 
			catch (Exception e) 
			{
				return null;
			}
		}
		
		/** 
		 * This method runs when doInBackground is finished.
		 * It informs the parent activity of it's result.
		 */
		protected void onPostExecute(RedditAccount account) 
		{
			
			tell_account(account);
		}
	}
	
	/**
	 * @author Arkady Eidelberg
	 * 
	 * This class sets up the recommendation engine.
	 *
	 */
	private class SetupPredictionIO extends AsyncTask<String, Void, PredIOConn> 
	{
		/** 
		 * This method runs when execture() is preformed.
		 * It takes no parameters.
		 */
		protected PredIOConn doInBackground(String... params) 
		{
			try 
			{
				// Create a new instance of the wrapper
				PredIOConn pio = new PredIOConn(APPURL, APPKEY_REC);
				// Get the status
				String s = pio.getStatus();
				
				// Inform parent activity of the status
				log_status_rec_engine(s);
				
				// Make sure the current user exists
				User user = pio.getUser(username);
				
				// Check if the user has visited the engine before. If not, add him!
				if (user != null)
				{
					pio.identify(username);
				}
				else
				{
					pio.addUser(username);
					pio.identify(username);
				}
				return pio;
			} 
			catch (Exception e) 
			{
				return null;
			}
		}
		
		/** 
		 * This method runs when doInBackground is finished.
		 * It informs the parent activity of it's result - log user and add / log the engine.
		 */
		protected void onPostExecute(PredIOConn pio) 
		{
			user_identified();
			add_rec_engine(pio);
		}
	}
	
	/**
	 * @author Arkady Eidelberg
	 * 
	 * This class gets recommendations from the engine.
	 *
	 */
	private class GetRecommendation extends AsyncTask<String, Void, String[]> 
	{
		/** 
		 * This method runs when execture() is preformed.
		 * It takes no parameters.
		 */
		protected String[] doInBackground(String... params) 
		{
			try 
			{
				// Get recommendations for the user
				String[] recommendations = rec_engine.getRec(username, REC_ENGINE_NAME, TARGET_POSTS);
				return recommendations;
			} 
			catch (Exception e) 
			{
				return null;
			}
		}
		
		
		/** 
		 * This method runs when doInBackground is finished.
		 * It informs the parent activity of it's result.
		 */
		protected void onPostExecute(String[] s) 
		{
			got_recommendations(s);
		}
	}
	
	/**
	 * @author Arkady Eidelberg
	 * 
	 * This class connects to reddit and grabs the body of a recommendation item.
	 *
	 */
	private class RecGrabber extends AsyncTask<String, Void, List<RedditLink>> 
	{
		/** 
		 * This method runs when execture() is preformed.
		 * It takes no parameters, but required recommendation_arr to be populated from before (or does nothing).
		 */
		protected List<RedditLink> doInBackground(String... params) 
		{
			try 
			{
				List<RedditLink> list = new ArrayList<RedditLink>();
				for (String item_id : recommendations_arr)
				{
					RedditLink link = reddit.getOneItem(item_id);
					list.add(link);
				}
				
				return list;
			} 
			catch (Exception e) 
			{
				return null;
			}
		}
		
		/** 
		 * This method runs when doInBackground is finished.
		 * It informs the parent activity of it's result.
		 * Also invokes to get the default behavior, for as many posts as the target is missing.
		 */
		protected void onPostExecute(List<RedditLink> list) 
		{
			rec_list = list;
			get_default_behaviour(TARGET_POSTS - list.size());
		}
	}
	
	/**
	 * @author Arkady Eidelberg
	 * 
	 * This class connects to reddit and grabs the body of a recommendation item.
	 *
	 */
	private class GetDefault extends AsyncTask<Integer, Void, List<RedditLink>> 
	{
		/** 
		 * This method runs when execture() is preformed.
		 * It takes no parameters.
		 */		
		protected List<RedditLink> doInBackground(Integer... params) 
		{
			try 
			{
				// Gets 25 posts by default.
				List<RedditLink> links = reddit.listingForMainPage();
				return links;
			} 
			catch (Exception e) 
			{
				return null;
			}
		}
		
		
		/** 
		 * This method runs when doInBackground is finished.
		 * It informs the parent activity of it's result.
		 */
		protected void onPostExecute(List<RedditLink> list) 
		{
			display_results(list);
		}
	}
	
	/**
	 * @author Arkady Eidelberg
	 * 
	 * This class connects to reddit and reports a vote made by the user.
	 *
	 */
	private class Voter extends AsyncTask<String, Void, String> 
	{
		// Constants
		public final static String UP_VOTE = "1";
		public final static String DOWN_VOTE = "-1";
		
		/** 
		 * This method runs when execture() is preformed.
		 * It takes no parameters.
		 */	
		
		protected String doInBackground(String... params) 
		{
			// param 0 is up or down vote
			String mode = params[0];
			// param 1 is the full name of the link to like or dislike
			String name = params[1];
			
			// Get singleton instance
			Reddit reddit = Reddit.getInstance("");
			
			// Preform up vote or down vote
			try
			{
				if (mode.equals(UP_VOTE))
				{
					reddit.vote(1, name);
				}
				else
				{
					reddit.vote(-1, name);
				}
				return "";
			}
			
			catch (Exception e)
			{
				return e.getMessage();
			}	
		}
		
		/** 
		 * This method runs when doInBackground is finished.
		 * It logs a successful vote or the exception error.
		 */
		protected void onPostExecute(String str) 
		{
			// This str happens from a successful vote - server sends back an empty JSON...
			if (str.equals("") || str.equals("JSON object to be parsed should not be empty!"))
				Log.d("ENGINE", "Snet vote!");
			else
				Log.e("ENGINE", str);
		}
	}
	
	/**
	 * @author Arkady Eidelberg
	 * 
	 * This class connects to the recommendation engine and reports a User2Item action
	 *
	 */
	private class EngineUpdater extends AsyncTask<String, Void, String> 
	{
		public final static String UP_VOTE = "up vote";
		public final static String VISIT = "visit";
		public final static String DOWN_VOTE = "down vote";
		
		/** 
		 * This method runs when execture() is preformed.
		 * It takes 3 parameters as a comma seperated list:
		 * 
		 * @param params[0] - the mode of the report, one of three static constants: UP_VOTE, VISIT or DOWN_VOTE. Defaults to DOWN_VOTE.
		 * @param params[1] - item name as known to reddit (same name on engine)
		 * @param params[2] - user name as known to reddit (same name on engine)
		 */	
		
		protected String doInBackground(String... params) 
		{
			// param 0 is up vote, visit or down vote
			String mode = params[0];
			// param 1 is the full name of the link to like or dislike
			String item_name = params[1];
			// param 2 is the name of the user
			String user_name = params[2];
	
			// Get the item from the engine to make sure it exists.
			Item item = rec_engine.getItem(item_name);
			
			// If item wasn't in the engine before add it
			if (item == null)
			{
				String[] itypes = {"main_page"};
				rec_engine.addItem(item_name, itypes);
				
				Log.d("ENGINE", "Added item " + item_name);
			}
			
			Log.d("ENGINE", "Item name was: " + item_name);
			
			// preform action
			if (mode.equals(UP_VOTE))
			{
				rec_engine.rate(user_name, item_name, 5);
			}
			else if (mode.equals(VISIT))
			{
				rec_engine.rate(user_name, item_name, 3);
			}
			else
			{
				rec_engine.rate(user_name, item_name, 1);
			}
		
			return mode;
		}
		
		/** 
		 * This method runs when doInBackground is finished.
		 * It informs the parent activity of it's result.
		 */
		protected void onPostExecute(String str) 
		{
			Log.d("ENGINE", "User sent an action to engine: " + str);
		}
	}
}