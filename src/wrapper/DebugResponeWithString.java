package wrapper;

import prediction.FutureAPIResponse;

public class DebugResponeWithString 
{
	/**
	 * This class is used internally to debug the system - it prints the response together with the string (the ID of the entity).
	 * 
	 * @param future_response - the FutureAPIResponse that was registered when the request was done.
	 * @param desc - the string ID of the item the request is handled for.
	 */
	
	private FutureAPIResponse future_response;
	private String desc;
	
	public DebugResponeWithString(FutureAPIResponse future_response, String desc)
	{
		this.setFuture_response(future_response);
		this.setDesc(desc);
	}

	public String toString()
	{
		return ("D: " + desc + " ||| M: " + future_response.getMessage());
	}
	
	// Getters and Setters
	
	public FutureAPIResponse getFuture_response() {
		return future_response;
	}

	public void setFuture_response(FutureAPIResponse future_response) {
		this.future_response = future_response;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
	
	
}
