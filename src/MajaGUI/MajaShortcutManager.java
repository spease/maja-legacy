package MajaGUI;
import javax.swing.*;

public class MajaShortcutManager
{
	public static class MajaShortcut
	{
		String _description;
		int _key = 0;
		int _modifier = 0;

		MajaShortcut(String n_description, int n_key)
		{
			_description = n_description;
			_key = n_key;
		}

		MajaShortcut(String n_description, int n_key, int n_modifier)
		{
			_description = n_description;
			_key = n_key;
			_modifier = n_modifier;
		}
	}
	
	public static interface Interface
	{
		public KeyStroke getAccelerator();
		public void setAccelerator(KeyStroke keyStroke);
	}
}