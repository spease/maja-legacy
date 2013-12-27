package Maja;
/**
* Classes inheriting from MajaUndoEvent should call super.undo() before their own undo() method.
*/
public class MajaUndoEvent
{
	private int _action;
	private String _description;
	
	private MajaUndoEvent _parent;
	private java.util.Vector<MajaUndoEvent> _subEvents = new java.util.Vector<MajaUndoEvent>();
	
	//Action defines
	public static final int NONE = 0;
	
	MajaUndoEvent(MajaUndoEvent n_parent, int n_action, String n_description)
	{
		_description = new String(n_description);
		_parent = n_parent;
		_action = n_action;
	}
	
	public void addSubevent(MajaUndoEvent e)
	{
		_subEvents.add(e);
	}
	
	public int getAction()
	{
		return _action;
	}
	
	public String getDescription()
	{
		return new String(_description);
	}
	
	public MajaUndoEvent getParent()
	{
		return _parent;
	}
	
	public boolean undo()
	{
		//Call all subevents first
		while(_subEvents.size() > 0)
		{
			MajaUndoEvent ue = _subEvents.lastElement();
			ue.undo();
			_subEvents.remove(ue);
		}
		
		return true;
	}
}