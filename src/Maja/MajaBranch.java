package Maja;
import java.util.*;

public abstract class MajaBranch<T extends MajaBranch>
{
	//*****Family variables
	MajaProject _project = null;
	T _parent = null;
	
	Vector<T> _children = new Vector<T>();
	
	//*****Functions
	public T addChild(T n_subentry)
	{
		if(n_subentry == null || n_subentry == this)
			return null;

		if(_children.contains(n_subentry))
			return n_subentry;
		
		//Check for recursion
		MajaBranch parent = this.getParent();
		while(parent != null)
		{
			if(parent == n_subentry)
				return null;
			
			parent = parent.getParent();
		}
		
		//Check for duplicate names
		/*
		T rtn = handleDuplicateChild(n_subentry);
		if(rtn != null)
			return rtn;
		*/
		
		//MajaUndoManager.postUndoEvent(this.createUndoEvent(MajaEntry.UndoEvent.ADD_SUBENTRY, n_subentry));
		_children.add(n_subentry);
		n_subentry.setParent(this);
		if(_project != null)
			_project.postEvent(MajaProject.EVENT_ADDED, n_subentry);
		return n_subentry;
	}
	
	protected void clearParent()
	{
		_parent = null;
	}
	
	public T getParent()
	{
		return _parent;
	}
	
	public abstract String getName();
	public abstract void setName(String n_name);
	
	public MajaProject getProject()
	{
		return _project;
	}
	
	public T getChild(int index)
	{
		return _children.get(index);
	}

	public T getChild(MajaIndex index)
	{
		if(index == null)
			return null;
		
		//I hate casts and don't understand why I need one here
		//YELLOW FLAG
		if(index.toString().equals(this.getName()))
			return (T)this;
		
		MajaIndex subindex = new MajaIndex(index);
		subindex.removeOuterLevel();

		for(int i = 0; i < _children.size(); i++)
		{
			String targetName = subindex.getOuterLevel();
			MajaBranch<T> sub = _children.get(i);
			if(sub.getName().equals(targetName))
			{
				//I hate casts and don't understand why I need one here
				//YELLOW FLAG
				return sub.getChild(subindex);
			}
		}

		return null;
	}

	public int getNumChildren()
	{
		return _children.size();
	}
	
	/**
	* Checks for duplicate entries an a vector of MajaEntries,
	* and adds subentries/modifies names accordingly.
	* If the function returns true, it means that all of the subentries
	* have been handled already.
	* @return false if the enclosing function should continue to add the entry, true otherwise
	*/
	public T handleDuplicateChild(T n_subentry)
	{
		for(int i = 0; i < _children.size(); i++)
		{
			T e = _children.get(i);
			if(		e.getName().equals(n_subentry.getName()))
			{
				if(e.equals(n_subentry))
				{
					for(int j = 0; j < n_subentry.getNumChildren(); j++)
					{
						e.addChild(n_subentry.getChild(j));
						MajaApp.displayWarning("Adding Child '" + n_subentry.getChild(j).getName() + "' to '" + e.getName() + "'");
					}
					return e;
				}
				else
				{
					/*if(n_entry.getSource() != null)
						n_entry.setName(n_subentry.getName() + ":" + n_subentry.getSource().getFileHandle().getName());
					else*/
					n_subentry.setName(n_subentry.getName() + ":null");
						
					MajaApp.displayWarning("Duplicate entry renamed to '" + n_subentry.getName() + "'");
				}
			}
		}
		
		return null;
	}
	
	public void remove()
	{
		if(this.getParent() != null)
		{
			this.getParent().removeChild(this);
		}
	}
	
	public void removeChild(T n_entry)
	{
		if(n_entry == null)
			return;

		if(_children.remove(n_entry))
		{
			//MajaUndoManager.postUndoEvent(this.createUndoEvent(MajaEntry.UndoEvent.REMOVE_SUBENTRY, n_entry));
			n_entry.clearParent();
		}
		
		if(_project != null)
			_project.postEvent(MajaProject.EVENT_REMOVED, n_entry);
	}
	
	/**
	*	Sets MajaBranch object's parent
	*/
	protected void setParent(T n_parent)
	{
		if(n_parent != null)
		{
			this.setProject(n_parent.getProject());
		}
		if(_parent != null)
		{
			_parent.removeChild(this);
		}
		_parent = n_parent;
	}
	
	/**
	* Sets project (recursive)
	*/
	protected void setProject(MajaProject n_project)
	{
		_project = n_project;
		for(int i = 0; i < this.getNumChildren(); i++)
		{
			this.getChild(i).setProject(n_project);
		}
	}
	
	public String toString()
	{
		return this.getName();
	}
}
