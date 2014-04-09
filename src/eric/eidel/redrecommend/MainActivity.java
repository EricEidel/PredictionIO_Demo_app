package eric.eidel.redrecommend;

import mapping.RedditJsonMessage;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * 
 * @author Arkady (Eric) Eidelberg
 * @email ninuson123@gmail.com
 * 
 * This is the main activity of the RedRecommend application.
 * 
 * This application connects a registered user of reddit both to the recommendation engine and to reddit.
 * You need to already have a registered user - but you can easily navigate to reddit and get it in a few seconds.
 * 
 * This activity only takes care of connecting to reddit. 
 * Reddit is a singleton that is shared between this activity and the next.
 *
 * Throughout the entire application there are many AsyncTask<?,?,?> classes. 
 * This was the only working solution I could find to connect to the internet.
 * 
 * A few adjustments might be needed to set this app working on another computer:
 * 1) In the RecommendeationPage activity, change the REC_ENGINE_NAME to the name of you engine.
 * 2) In the same activity, change the APPKEY_REC to the app key.
 * 
 *  After that, the app should run as normal.
 */

public class MainActivity extends Activity 
{
	// Constants to pass intent
	static String USER_NAME = "USERNAME_RED_RECOMEND";
	static String PASSWORD = "PASSWORD_RED_RECOMEND";
	static String REDDIT = "REDDIT_RED_RECOMEND";
	
	// needed to handle exception later
	public Exception exception;
	
	
	/**
	 * This is the entrance point to the code.
	 * 
	 * Set up the layout, fill in the text fields with a mock user.
	 * Set up the log in button listener.
	 */
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Get the layout set up
        TextView label = (TextView) findViewById(R.id.textView1);
        label.setVisibility(TextView.INVISIBLE);
        
        EditText user_field= (EditText) findViewById(R.id.username_field);
        user_field.setText("testing_122");
        
        EditText pass_field= (EditText) findViewById(R.id.password_field);
        pass_field.setText("1234");
        
        // Log in button and listener
        Button button= (Button) findViewById(R.id.loging_button);
        button.setOnClickListener(new View.OnClickListener() 
        {
            @Override
            public void onClick(View v) 
            {
         	    // Login with a user
         	    LogInUser log_in_task = new LogInUser();
         	    
         	    // pass over the username 
	        	EditText editText = (EditText) findViewById(R.id.username_field);
	        	String user = editText.getText().toString();
	        	
	        	// pass over the password 
	        	editText = (EditText) findViewById(R.id.password_field);
	        	String password = editText.getText().toString();
	        	
	        	// Do the reddit stuff in another thread
         		log_in_task.execute(user, password);        	     	
            }
        }); 
    }

    /** 
     * Not needed, required by android
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    /**
	 * This method is called internally when a user successfuly logs to reddit through the API.
	 * This is a successful use-case of pushing the log in button.
	 */
	protected void done_setting_up()
	{
		// If the username and password are ok, connect to the main page of the application
    	Intent intent = new Intent(MainActivity.this, RecommendationPage.class);
    	
    	// pass over the username 
    	EditText editText = (EditText) findViewById(R.id.username_field);
    	String message = editText.getText().toString();
    	intent.putExtra(USER_NAME, message);
    	
    	startActivity(intent);
	}
	
	/**
	 * This method gets invoked when a response comes from reddit server about the user's attemp to log in.
	 * @param msg - Reddit's response.
	 * 
	 * In case of successful log in, the next activity will be loaded through done_seeting_up.
	 * Otherwise, display an error label on the top of the screen and let the user try again.
	 */
	protected void came_in(RedditJsonMessage msg)
	{
    	if (exception != null)
    	{
    		TextView label = (TextView) findViewById(R.id.textView1);
	        label.setVisibility(TextView.VISIBLE);
	        
    		Log.e("ENGINE", exception.getMessage());
    		exception = null;
    	}
    	else
    	{
    		//Log.d("ENGINE", msg.toString());
    		TextView label = (TextView) findViewById(R.id.textView1);
	        label.setTextColor(Color.GREEN);
	        label.setText("Logging in...");
	        
    		done_setting_up();
    	}
    	
	}
	
    /** AsyncTask for logging in the user.
     * 
     * @author Arkady Eidelberg
     *
     */
    private class LogInUser extends AsyncTask<String, Void, RedditJsonMessage> 
    {
    	/**
    	 * This is going in the background.
    	 * @param params - comma seperated list of arguments. 
    	 * params[0] - user name for the user trying to log in.
    	 * params[1] - password for that user.
    	 */
	    protected RedditJsonMessage doInBackground(String... params) 
	    {
	        try 
	        {
	        	// Get user name
	        	String user = params[0];
	        	// Get password
	        	String password = params[1];
	        	
	        	// Reset the singleton, needed to enable logging out and logging in as a different user again.
	        	Reddit.reset();
	        	
	        	// Set the singleton to have an instance of the current user.
	        	Reddit reddit = Reddit.getInstance(user);
	        	        	
	        	Log.d("ENGINE", "Trying to log in as:" + user + " / " + password);
	        	
	        	// Get the response from reddit
	        	RedditJsonMessage response = reddit.login( user, password );
	            return response;
	        } 
	        catch (Exception e) 
	        {
	        	exception = e;
	            return null;
	        }
	    }
	    
	    /**
	     * Once the response comes, notify the UI thread for further action.
	     */
	    protected void onPostExecute(RedditJsonMessage msg) 
	    {
	    	came_in(msg);
	    }
	}
}
