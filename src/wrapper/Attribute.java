package wrapper;

public class Attribute 
{
	private String name;
	private String value;
	
	/**
	 * 	A simple <name, value> pair.
	 *	Used to represent an optional attribute of an item
	 *
	 *	@param name this is the name of the attribute field.
	 *	@param value this is the value of the attribute field.
	 */
	
	public Attribute(String name, String value)
	{
		this.setName(name);
		this.setValue(value);
	}

	// Getters and setters
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	
}
