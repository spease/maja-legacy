package Maja;
public class MajaIndex
{
	private String _string = new String();

	public MajaIndex(MajaIndex n_i)
	{
		if(n_i != null)
			_string = new String(n_i.toString());
	}

	public MajaIndex(String n_s)
	{
		if(n_s != null)
			_string = new String(n_s);
	}

	public boolean equals(MajaIndex i)
	{
		if(i == null)
			return false;
		
		return (_string.equals(i._string));
	}
	public void addLevel(String n_s)
	{
		if(n_s == null)
			return;
		
		if(_string.length() < 1)
		{
			_string = new String(n_s);
			return;
		}

		_string = _string + "/" + n_s;
	}

	public String getInnerLevel()
	{
		int lastIndex = _string.lastIndexOf('/');
		if(lastIndex == -1)
			return new String(_string);

		return _string.substring(lastIndex + 1);
	}

	public String getOuterLevel()
	{
		int index = _string.indexOf('/');
		if(index == -1)
			return new String(_string);
		
		return _string.substring(0, index);
	}

	/**
	* Removes rightmost item
	*/
	public void removeInnerLevel()
	{
		int lastindex = _string.lastIndexOf('/');

		if(lastindex == -1)
		{
			_string = "";
			return;
		}

		_string = _string.substring(0, lastindex);
	}

	/**
	* Removes leftmost item
	*/
	public void removeOuterLevel()
	{
		int index = _string.indexOf('/');
		if(index == -1)
		{
			_string = "";
			return;
		}
		
		_string = _string.substring(index + 1);
	}

	public String toString()
	{
		return new String(_string);
	}
}