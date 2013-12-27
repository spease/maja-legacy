package MajaGUI;
import javax.swing.*;
import java.awt.event.*;
import Maja.*;

public class MajaComponentEntriesTree extends MajaViewport.ComponentPanel
{
	private static final long serialVersionUID = -3226922291177166715L;
	JScrollPane _scrollPane;

	MajaComponentEntriesTree(MajaFrame n_majaFrame, MajaProjectTabs.ProjectPanel tp, MajaProject p)
	{
		super(n_majaFrame, tp, p, "[1] Entries Tree");
		//this.setLayout(new java.awt.BorderLayout());

		//*****Create tree
		MajaComponentEntriesTree.EntriesTree tree = new MajaComponentEntriesTree.EntriesTree(p);

		_scrollPane = new JScrollPane(tree);

		this.add(_scrollPane, java.awt.BorderLayout.CENTER);
		//this.add(tree, java.awt.BorderLayout.CENTER);
		this.setVisible(true);
	}

	public class EntriesTree extends JTree implements
		MajaProject.MajaEventListener,
		javax.swing.event.TreeSelectionListener, javax.swing.event.TreeModelListener,
		java.awt.event.MouseListener, java.awt.event.ActionListener,
		java.awt.dnd.DragGestureListener, java.awt.dnd.DragSourceListener,
		java.awt.dnd.DropTargetListener
	{
		private static final long serialVersionUID = -1692519272867698226L;
		MajaProject _project;
		//MajaProjectTreeNode _root;
		MajaEntryTreeNode _root;
		javax.swing.tree.DefaultTreeModel _model;
		//javax.swing.tree.DefaultTreeSelectionModel _selectionModel;
		
		java.awt.dnd.DragSource _dragSource;
		java.awt.dnd.DragGestureRecognizer _dragGestureRecognizer;

		EntriesTree(MajaProject p)
		{
			super();
			_project = p;
			//_root = new MajaProjectTreeNode(_project);
			_root = new MajaEntryTreeNode(_project.getEntryHead());
			_model = new javax.swing.tree.DefaultTreeModel(_root);
			//_selectionModel = new javax.swing.tree.DefaultTreeSelectionModel();
			
			this.setModel(_model);
			//this.setSelectionModel(_selectionModel);
			
			this.setScrollsOnExpand(false);
			/*
			for(int i = 0; i < p.getNumEntries(true); i++)
			{
				MajaEntryTreeNode currentNode = this.addEntry(_root, p.getEntry(i));
				this.setExpandedState(new javax.swing.tree.TreePath(currentNode.getPath()), true);
			}*/
			
			//*****Add keystrokes
			//this.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
			//this.getActionMap().put("delete", this);
			//This may be "obsolete", but it's much more efficient than the new method
			this.registerKeyboardAction(this, "remove", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
			
			//*****Setup drag and drop
			_dragSource = java.awt.dnd.DragSource.getDefaultDragSource();
			_dragGestureRecognizer = _dragSource.createDefaultDragGestureRecognizer(
				this,
				java.awt.dnd.DnDConstants.ACTION_COPY_OR_MOVE,
				this
			);
			java.awt.dnd.DropTarget dropTarget = new java.awt.dnd.DropTarget(this, this);
				
			
			//*****Set default properties
			this.setScrollsOnExpand(true);
			this.setEditable(true);
			this.getSelectionModel().setSelectionMode(javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION);
			
			//Listeners
			this.addTreeSelectionListener(this);
			_model.addTreeModelListener(this);
			//p.addUndoListener(this);
			p.addEventListener(this);
			this.addMouseListener(this);
		}

		private MajaEntryTreeNode addEntry(javax.swing.tree.DefaultMutableTreeNode top, MajaEntry e)
		{
			if(top == null || e == null)
				return null;

			MajaEntryTreeNode currentNode = new MajaEntryTreeNode(e);
			_model.insertNodeInto(currentNode, top, top.getChildCount());
			
			if(e.getType() == MajaEntry.FOLDER)
			{
				this.makeVisible(new javax.swing.tree.TreePath(currentNode.getPath()));
			}
			
			for(int i = 0; i < e.getNumChildren(); i++)
			{
				this.addEntry(currentNode, e.getChild(i));
			}
			
			return currentNode;
		}
		
		/**
		* Finds an entry from the given tree node
		* 
		* @param	top				The tree node to start from
		* @param	e				Entry to find
		* @return					Tree node of entry specified, or null if not found
		*/
		public MajaEntryTreeNode findEntry(javax.swing.tree.DefaultMutableTreeNode top, MajaEntry e)
		{
			if(top instanceof MajaEntryTreeNode)
			{
				MajaEntryTreeNode metn_top = (MajaEntryTreeNode) top;
				if(metn_top.getEntry() == e)
				{
					/*
					Object parentNode = metn_top.getParent();
					if(parentNode instanceof MajaEntryTreeNode && parent != null)
					{
						if(((MajaEntryTreeNode)parentNode).getEntry() == parent)
						{
							return metn_top;
						}
					}
					else if(parentNode instanceof MajaProjectTreeNode)
					{
						if(parent == null && ((MajaProjectTreeNode)parentNode).getProject() == e.getProject())
						{
							return metn_top;
						}
					}
					*/
					return metn_top;
				}
			}
				
			MajaEntryTreeNode retval = null;

			for(int i = 0; i < top.getChildCount(); i++)
			{
				retval = this.findEntry((javax.swing.tree.DefaultMutableTreeNode)top.getChildAt(i), e);
				if(retval != null)
					break;
			}
			
			return retval;
		}
		
		public void removeEntry(MajaEntry e)
		{
			if(e == null)
			{
				MajaApp.displayWarning("Attempted to remove null entry");
			}
			else
			{
				//MajaApp.displayWarning("Attempting removal of '" + e.getName() + "' from tree");
			}
			MajaEntryTreeNode node = this.findEntry(_root, e);
			if(node != null)
			{
				_model.removeNodeFromParent(node);
			}
		}
		//***************-----INTERFACE: MajaEventListener-----***************//
		public void processEvent(int e, MajaEntry me)
		{
			switch(e)
			{
				case MajaProject.EVENT_ADDED:
				{
					this.addEntry(this.findEntry(_root, me.getParent()), me);
					break;
				}
				case MajaProject.EVENT_REMOVED:
					this.removeEntry(me);
					break;
				case MajaProject.EVENT_UPDATED:
				{
					MajaEntryTreeNode metn = this.findEntry(_root, me);
					if(metn != null)
					{
						metn.setText(me.getName());
						this.makeVisible(new javax.swing.tree.TreePath(metn.getPath()));
					}
					break;
				}
				default:
					MajaApp.displayWarning("MCET: Unknown message from MajaEntry '" + me.getName() + "'");
			}
		}
		public void processEvent(int e, MajaProject mp)
		{
			switch(e)
			{
				case MajaProject.EVENT_UPDATED:
				{
					MajaEntryTreeNode metn = this.findEntry(_root, mp.getEntryHead());
					if(metn != null)
					{
						metn.setText(mp.getName());
						this.makeVisible(new javax.swing.tree.TreePath(metn.getPath()));
					}
					break;
				}
				default:
					MajaApp.displayWarning("MCET: Unknown message from MajaProject '" + mp.getName() + "'");
			}
		}
		public void processEvent(int e, MajaSource ms){}
		public void processEvent(int e, MajaSourceEntry mse){}

		//*****UNDO LISTENER
		/*
		public void undoEventPosted(MajaUndoEvent e){}
		public void majaEntryUndoEventPosted(MajaEntry.UndoEvent e)
		{
			switch(e.getAction())
			{
				case MajaEntry.UndoEvent.ADD_SUBENTRY:
				{
					MajaEntryTreeNode node = this.addEntry(this.findEntry(_root, e.getTarget(), e.getTarget().getParent()), e.getBuffer());
//					if(node != null)
//					{
//						javax.swing.tree.TreeNode[] tn = node.getPath();
//						if(tn != null)
//							this.makeVisible(new javax.swing.tree.TreePath(tn));
					}
					break;
				}
				case MajaEntry.UndoEvent.REMOVE_SUBENTRY:
				{
					this.removeEntry(e.getBuffer(), e.getBuffer().getParent());
					break;
				}
				case MajaEntry.UndoEvent.SET_NAME:
				{
					MajaEntryTreeNode metn = this.findEntry(_root, e.getTarget(), e.getTarget().getParent());
					metn.setText(e.getBuffer().getName());
					this.expandPath(new javax.swing.tree.TreePath(metn.getPath()));
					break;
				}
			}
		}
		public void majaProjectUndoEventPosted(MajaProject.UndoEvent e)
		{
			//MajaApp.displayStatus("majaProjectUndoEventPosted activated with event " + e.getDescription());
			switch(e.getAction())
			{
				case MajaProject.UndoEvent.ADD_ENTRY:
				{
					MajaEntryTreeNode node = this.addEntry(_root, e.getMajaEntryBuffer());
					this.makeVisible(new javax.swing.tree.TreePath(node.getPath()));
					break;
				}
				case MajaProject.UndoEvent.REMOVE_ENTRY:
				{
					MajaEntry me = e.getMajaEntryBuffer();
					this.removeEntry(me, null);
					break;
				}
			}
		}
		*/
		
		//***************-----INTERFACE: TreeModelListener-----***************//
		public void treeNodesChanged(javax.swing.event.TreeModelEvent e)
		{
			javax.swing.tree.DefaultMutableTreeNode node = (javax.swing.tree.DefaultMutableTreeNode)e.getTreePath().getLastPathComponent();
			try
			{
				int index = e.getChildIndices()[0];
				node = (javax.swing.tree.DefaultMutableTreeNode) (node.getChildAt(index));
			} catch (NullPointerException npe){}
			
			//This seems to completely overwrite the UserObject of DefaultMutableTreeNodes.
			//MajaApp.displayStatus("New name: '" + node.getUserObject().toString() + "'");
			if(node instanceof MajaEntryTreeNode && node.getUserObject() instanceof String)
			{
				MajaEntryTreeNode metn_node = (MajaEntryTreeNode) node;
				MajaEntry me = metn_node.getEntry();
				if(me.getType() == MajaEntry.HEAD)
				{
					me.getProject().setName((String)node.getUserObject());
				}
				else
				{
					me.setName((String)node.getUserObject());
				}
			}
			else if(node instanceof MajaProjectTreeNode && node.getUserObject() instanceof String)
			{
				MajaProjectTreeNode mptn_node = (MajaProjectTreeNode) node;
				mptn_node.getProject().setName((String)node.getUserObject());
			}
		}
		public void treeNodesInserted(javax.swing.event.TreeModelEvent e) {}
		public void treeNodesRemoved(javax.swing.event.TreeModelEvent e) {}
		public void treeStructureChanged(javax.swing.event.TreeModelEvent e) {}
		
		//***************-----INTERFACE: TreeSelectionListener-----***************//
		public void valueChanged(javax.swing.event.TreeSelectionEvent e)
		{
			javax.swing.tree.TreePath path = e.getNewLeadSelectionPath();
			if(path != null)
			{
				Object o = path.getLastPathComponent();
				if(o instanceof MajaEntryTreeNode)
				{
					//MajaEntryTreeNode node = (MajaEntryTreeNode) o;
					//_majaFrame.setSelectedEntry(node.getEntry());
				}
			}
		}
		
		//*****MOUSE LISTENER
		public void mouseShowPopup(java.awt.event.MouseEvent e)
		{
			if(!e.isPopupTrigger())
				return;

			JTree tree = (JTree)e.getSource();
			javax.swing.tree.TreePath path = tree.getPathForLocation(e.getX(), e.getY());
			if(path != null)
			{
				Object o = path.getLastPathComponent();
				if(o instanceof javax.swing.tree.DefaultMutableTreeNode)
				{
					TreeItemPopupMenu pop = null;
					javax.swing.tree.DefaultMutableTreeNode node = (javax.swing.tree.DefaultMutableTreeNode) o;
					if(node instanceof MajaEntryTreeNode)
					{
						MajaEntry me = ((MajaEntryTreeNode)node).getEntry();
						pop = new TreeItemPopupMenu(node, me.getProject(), me);
					}
					else if(node instanceof MajaProjectTreeNode)
					{
						MajaProject mp = ((MajaProjectTreeNode)node).getProject();
						pop = new TreeItemPopupMenu(node, mp, null);
					}
					
					if(pop != null)
					{
						//*****Show popup menu
						pop.show(e.getComponent(), e.getX(), e.getY());
						//MajaApp.displayStatus("Clicked on '" + ((javax.swing.tree.DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject() + "'");
					}
				}
			}
		}
		public void mousePressed(java.awt.event.MouseEvent e)
		{
			mouseShowPopup(e);
		}
		public void mouseReleased(java.awt.event.MouseEvent e)
		{
			mouseShowPopup(e);
		}
		public void mouseClicked(java.awt.event.MouseEvent e)
		{
			//Open this in an edit window
			if(e.getClickCount() < 2)
				return;
			
			JTree tree = (JTree)e.getSource();
			javax.swing.tree.TreePath path = tree.getPathForLocation(e.getX(), e.getY());
			if(path != null)
			{
				Object o = path.getLastPathComponent();
				if(o instanceof MajaEntryTreeNode)
				{
					MajaEntry me = ((MajaEntryTreeNode)o).getEntry();
					_projectPanel.setTarget(me);
					//MajaApp.displayStatus("Trying to open '" + me.toString() + "'");
				}
			}
		}
		public void mouseEntered(java.awt.event.MouseEvent e) {}
		public void mouseExited(java.awt.event.MouseEvent e) {}
		
		//********************DropTargetListener********************
		public void dragEnter(java.awt.dnd.DropTargetDragEvent dtde){}
		public void dragExit(java.awt.dnd.DropTargetEvent dte){}
		public void dragOver(java.awt.dnd.DropTargetDragEvent dtde)
		{
			java.awt.datatransfer.Transferable transferableObject = dtde.getTransferable();
			
			boolean majaDFSupported = transferableObject.isDataFlavorSupported(MajaEntry.getDataFlavor());
			boolean fileListDFSupported = transferableObject.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
			
			boolean drop_possible = false;
			java.awt.Point p = dtde.getLocation();
			javax.swing.tree.TreePath destinationPath = getPathForLocation(p.x, p.y);
			if(destinationPath != null)
			{
				Object o = destinationPath.getLastPathComponent();
				if(o instanceof MajaEntryTreeNode)
				{
					MajaEntryTreeNode node = (MajaEntryTreeNode) o;
					if(node.getAllowsChildren())
						drop_possible = true;
				}
				else if(o instanceof MajaProjectTreeNode)
				{
					drop_possible = true;
				}
			}
			
			if(drop_possible)
			{
				if(majaDFSupported)
				{
					dtde.acceptDrag(java.awt.dnd.DnDConstants.ACTION_MOVE | java.awt.dnd.DnDConstants.ACTION_COPY);
				}
				else if(fileListDFSupported)
				{
					dtde.acceptDrag(java.awt.dnd.DnDConstants.ACTION_LINK);
				}
				else
				{
					dtde.rejectDrag();
				}
			}
			else
			{
				dtde.rejectDrag();
			}
		}
		public void drop(final java.awt.dnd.DropTargetDropEvent dtde)
		{
			java.awt.datatransfer.Transferable transferableObject = dtde.getTransferable();
			
			boolean majaDFSupported = transferableObject.isDataFlavorSupported(MajaEntry.getDataFlavor());
			boolean fileListDFSupported = transferableObject.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
			if(!(majaDFSupported || fileListDFSupported))
			{
				MajaApp.displayWarning("Bad data flavor for drag and drop");
				dtde.rejectDrop();
				return;
			}

			java.awt.Point p = dtde.getLocation();
			javax.swing.tree.TreePath destinationPath = this.getPathForLocation(p.x, p.y);
			
			if(destinationPath == null)
			{
				dtde.rejectDrop();
				return;
			}
			javax.swing.tree.DefaultMutableTreeNode newParent = (javax.swing.tree.DefaultMutableTreeNode)destinationPath.getLastPathComponent();

			try
			{
				if(majaDFSupported)
				{
					MajaEntry majaEntry = (MajaEntry)transferableObject.getTransferData(MajaEntry.getDataFlavor());
					MajaEntry finalEntry = null;
					
					if(majaEntry.getProject() != _project)
					{
						MajaApp.displayWarning("Inter-project drag and drop is currently not supported.");
						dtde.rejectDrop();
						return;
					}
					
					switch(dtde.getDropAction())
					{
						case java.awt.dnd.DnDConstants.ACTION_COPY:
						{
							try
							{
								//Make a copy
								finalEntry = majaEntry.clone();
							}
							catch(CloneNotSupportedException ex)
							{
								MajaApp.displayWarning("Unable to clone object");
								dtde.rejectDrop();
								return;
							}
							MajaApp.displayStatus("Copying " + finalEntry.getName());
							break;
						}
						case java.awt.dnd.DnDConstants.ACTION_MOVE:
						{
							finalEntry = majaEntry;
							//majaEntry.getProject().removeEntry(majaEntry);
							MajaApp.displayStatus("Moving " + finalEntry.getName());
							break;
						}
					}
					
					if(newParent instanceof MajaEntryTreeNode)
					{
						((MajaEntryTreeNode)newParent).getEntry().addChild(finalEntry);
					}
					else if(newParent instanceof MajaProjectTreeNode)
					{
						((MajaProjectTreeNode)newParent).getProject().addEntry(finalEntry);
					}
				}
				else if(fileListDFSupported)
				{
					dtde.acceptDrop(dtde.getSourceActions());
					Object transferData = transferableObject.getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
					if(transferData instanceof java.util.List)
					{
						final java.util.List fileList = (java.util.List) transferData;
						MajaEntry tempParentEntry = null;
						if(newParent instanceof MajaEntryTreeNode)
						{
							tempParentEntry = ((MajaEntryTreeNode)newParent).getEntry();
						}
						
						//Make these final
						final MajaEntry parentEntry = tempParentEntry;
						
						Thread t = new Thread("Package(s) drag import")
						{
							public void run()
							{
								for(int i = 0; i < fileList.size(); i++)
								{
									Object o = fileList.get(i);
									if(o instanceof java.io.File)
									{
										java.io.File f = (java.io.File) o;
										if(f.isFile())
										{
											MajaIO.MajaHandlerManager.MajaHandler mh = MajaIO.MajaHandlerManager.findPackageHandler(f);
											if(mh == null)
											{
												_project.importFile(parentEntry, f);
											}
											else
											{
												//We could import this as a package, so go ahead and ask.
												final String options[] = {
													"Import as package",
													"Import as file",
													"Cancel",
												};
												int result = JOptionPane.showOptionDialog(
													_majaFrame,
													"Maja has detected that '" + f.getName() + "' is a package file. "
														+ "Do you want to add it as a package or as a file?",
													"Importing '" + f.getName() + "'",
													JOptionPane.YES_NO_CANCEL_OPTION,
													JOptionPane.QUESTION_MESSAGE,
													null,
													options,
													options[2]);
												if(result == JOptionPane.YES_OPTION)
													_project.importPackage(parentEntry, f);
												else if(result == JOptionPane.NO_OPTION)
													_project.importFile(parentEntry, f);
												else if(result == JOptionPane.CANCEL_OPTION)
													{}
											}
										}
										else if(f.isDirectory())
											_project.importDirectory(parentEntry, f);
										else
											MajaApp.displayWarning("Could not add drag-and-drop item '" + f.getName() + "' as it was not a file or directory");
									}
								}
								dtde.dropComplete(true);
							}
						};
						t.start();
					}
					else
					{
						MajaApp.displayWarning("Bad object type for data flavor javaFileListFlavor");
						dtde.dropComplete(false);
					}
				}
			}
			catch(java.awt.datatransfer.UnsupportedFlavorException ex)
			{
				MajaApp.displayException(ex, "Unable to get proper flavor to drop item.");
				dtde.rejectDrop();
				return;
			}
			catch(java.io.IOException ex)
			{
				MajaApp.displayException(ex, "IO Exception in DnD code");
				dtde.rejectDrop();
				return;
			}
		}
		public void dropActionChanged(java.awt.dnd.DropTargetDragEvent dtde){}
		
		//********************DragGestureListener********************
		public void dragGestureRecognized(java.awt.dnd.DragGestureEvent dge)
		{
			//MajaApp.displayStatus("Drag gesture recognized");
			if(this.getSelectionCount() != 1)
				return; 

			Object o = this.getSelectionPath().getLastPathComponent();
			if(!(o instanceof MajaEntryTreeNode))
				return;
				
			MajaEntryTreeNode node = (MajaEntryTreeNode) o;
			MajaEntry majaEntry = node.getEntry();

			java.awt.Cursor cursor = java.awt.dnd.DragSource.DefaultCopyNoDrop;
			
			dge.startDrag(cursor, majaEntry, this);
		}
		
		//********************DragSourceListener********************
		public void dragDropEnd(java.awt.dnd.DragSourceDropEvent dsde)
		{
			java.awt.dnd.DragSourceContext dsc = dsde.getDragSourceContext();
			if(dsde.getDropSuccess() && dsde.getDropAction() == java.awt.dnd.DnDConstants.ACTION_MOVE)
			{
				java.awt.datatransfer.Transferable t = dsc.getTransferable();
				if(t instanceof MajaEntry)
				{
					MajaEntry me = (MajaEntry) t;
					me.remove();
				}
			}
		}
		public void dragEnter(java.awt.dnd.DragSourceDragEvent dsde)
		{
			dsde.getDragSourceContext().setCursor(getDragDropCursor(dsde.getDropAction(), dsde.getTargetActions()));
		}
		public void dragExit(java.awt.dnd.DragSourceEvent dse)
		{
			dse.getDragSourceContext().setCursor(getDragDropCursor(java.awt.dnd.DnDConstants.ACTION_NONE, java.awt.dnd.DnDConstants.ACTION_NONE));
		}
		public void dragOver(java.awt.dnd.DragSourceDragEvent dsde)
		{
			dsde.getDragSourceContext().setCursor(getDragDropCursor(dsde.getDropAction(), dsde.getTargetActions()));
		}
		public void dropActionChanged(java.awt.dnd.DragSourceDragEvent dsde)
		{
			dsde.getDragSourceContext().setCursor(getDragDropCursor(dsde.getDropAction(), dsde.getTargetActions()));
		}
		
		public java.awt.Cursor getDragDropCursor(int dropAction, int targetUserAction)
		{
			java.awt.Cursor cursor = java.awt.dnd.DragSource.DefaultMoveNoDrop;
			boolean possible = true;
			switch(dropAction)
			{
				case java.awt.dnd.DnDConstants.ACTION_LINK:
					//possible = ((dropAction & java.awt.dnd.DnDConstants.ACTION_LINK) & targetUserAction) > 0;
					cursor = possible ? java.awt.dnd.DragSource.DefaultLinkDrop : java.awt.dnd.DragSource.DefaultLinkNoDrop;
					break;
				case java.awt.dnd.DnDConstants.ACTION_COPY:
					//possible = ((dropAction & java.awt.dnd.DnDConstants.ACTION_COPY) & targetUserAction) > 0;
					cursor = possible ? java.awt.dnd.DragSource.DefaultCopyDrop : java.awt.dnd.DragSource.DefaultCopyNoDrop;
					break;
				case java.awt.dnd.DnDConstants.ACTION_MOVE:
					//possible = ((dropAction & java.awt.dnd.DnDConstants.ACTION_MOVE) & targetUserAction) > 0;
					cursor = possible ? java.awt.dnd.DragSource.DefaultMoveDrop : java.awt.dnd.DragSource.DefaultMoveNoDrop;
					break;
				default:
			}
			//MajaApp.displayStatus("Updating cursor");
			
			return cursor;
		}
		
		//*****ACTION
		//For keystrokes
		public void actionPerformed(java.awt.event.ActionEvent e)
		{
			String a = e.getActionCommand();
			if(a.equals("remove"))
			{	
				javax.swing.tree.TreePath[] paths = this.getSelectionPaths();
				if(paths != null)
				{
					for(int i = 0; i < paths.length; i++)
					{
						Object o = paths[i].getLastPathComponent();
						if(o instanceof MajaEntryTreeNode)
						{
							MajaEntry me = ((MajaEntryTreeNode)o).getEntry();
							_majaFrame.removeEntry(me);
						}
					}
				}
			}
		}
		
		public class MajaEntryTreeNode extends javax.swing.tree.DefaultMutableTreeNode
		{
			private static final long serialVersionUID = -1803169481057704321L;
			MajaEntry _majaEntry;
			
			/**
			* Creates a new node; n_majaEntry must not be null.
			*/
			public MajaEntryTreeNode(MajaEntry n_majaEntry)
			{
				super(n_majaEntry.getType() != MajaEntry.HEAD ? n_majaEntry.getName() : n_majaEntry.getProject().getName(), n_majaEntry.getType() == MajaEntry.FOLDER || n_majaEntry.getType() == MajaEntry.HEAD);
				
				_majaEntry = n_majaEntry;
			}
			
			public MajaEntryTreeNode getChild(int i)
			{
				javax.swing.tree.TreeNode tn = null;
				try {
					tn = this.getChildAt(i);
				} catch(ArrayIndexOutOfBoundsException exc) {
					return null;
				}
				
				if(tn == null || !(tn instanceof MajaEntryTreeNode))
					return null;
					
				return (MajaEntryTreeNode) tn;
			}
			
			/**
			* Returns associated MajaEntry
			*/
			public MajaEntry getEntry()
			{
				return _majaEntry;
			}
			
			/**
			* Returns false for folders, true for files, to force proper icons.
			*/
			public boolean isLeaf()
			{
				if(this.getAllowsChildren())
				{
					return false;
				}
				
				return (_majaEntry.getNumChildren() < 1);
			}
			
			/**
			* Sets node name
			*/
			public void setText(String s)
			{
				this.setUserObject(new String(s));
				EntriesTree.this.treeDidChange();
			}
		}
		
		public class MajaProjectTreeNode extends javax.swing.tree.DefaultMutableTreeNode
		{
			private static final long serialVersionUID = -8243676219668059746L;
			MajaProject _majaProject;
			
			/**
			* Creates a new node; n_majaProject must not be null.
			*/
			public MajaProjectTreeNode(MajaProject n_majaProject)
			{
				super(n_majaProject.getName(), true);
				
				_majaProject = n_majaProject;
			}
			
			/**
			* Returns associated MajaProject
			*/
			public MajaProject getProject()
			{
				return _majaProject;
			}
			
			/**
			* Returns false for folders, true for files, to force proper icons.
			*/
			public boolean isLeaf()
			{
				return false;
			}
		}
		
		public class TreeItemPopupMenu extends JPopupMenu implements ActionListener
		{
			private static final long serialVersionUID = 1527819107769790177L;
			javax.swing.tree.DefaultMutableTreeNode _treeNode;
			MajaProject _majaProject;
			MajaEntry _majaEntry;
			
			JMenuItem _menuNewFile;
			JMenuItem _menuNewFolder;
			JMenuItem _menuImportFile;
			JMenuItem _menuImportDirectory;
			JMenuItem _menuImportPackage;
			JMenuItem _menuSync;
			JMenuItem _menuRename;
			JMenuItem _menuEdit;
			JMenuItem _menuExportDirectory;
			JMenuItem _menuExportFile;
			JMenuItem _menuExportPackage;
			JMenuItem _menuDetails;
			JMenuItem _menuDelete;
			public TreeItemPopupMenu(javax.swing.tree.DefaultMutableTreeNode n_treeNode, MajaProject n_majaProject, MajaEntry n_majaEntry)
			{
				super();
				_treeNode = n_treeNode;
				_majaProject = n_majaProject;
				_majaEntry = n_majaEntry;
				
				if(_majaEntry == null)
					this.setLabel("<Root>");
				else
					this.setLabel(_majaEntry.getName());

				if(n_treeNode.getAllowsChildren())
				{
					if(_majaProject != null && _majaProject.getOutputPath() != null)
					{
						_menuNewFile = new JMenuItem("New file...");
						_menuNewFile.addActionListener(this);
						this.add(_menuNewFile);
					}
					
					_menuNewFolder = new JMenuItem("New folder...");
					_menuNewFolder.addActionListener(this);
					this.add(_menuNewFolder);
					
					this.addSeparator();

					_menuImportFile = new JMenuItem("Import File(s)...");
					_menuImportFile.addActionListener(this);
					this.add(_menuImportFile);
					
					_menuImportDirectory = new JMenuItem("Import Directory...");
					_menuImportDirectory.addActionListener(this);
					this.add(_menuImportDirectory);
					
					_menuImportPackage = new JMenuItem("Import Package(s)...");
					_menuImportPackage.addActionListener(this);
					this.add(_menuImportPackage);
					
					this.addSeparator();
				}

				_menuRename = new JMenuItem("Rename");
				_menuRename.addActionListener(this);
				this.add(_menuRename);

				if(_majaEntry != null)
				{
					if(_majaEntry.getType() == MajaEntry.FILE)
					{
						_menuEdit = new JMenuItem("Edit");
						_menuEdit.addActionListener(this);
						this.add(_menuEdit);
					}
				}

				if((_majaEntry == null && _majaProject.getNumEntries(false) > 0) || (_majaEntry != null && (_majaEntry.getSourceEntry() != null || _treeNode.getAllowsChildren())))
				{
					this.addSeparator();
					
					if(_majaEntry != null && _majaEntry.getStatus() == MajaEntry.CONNECTED)
					{
						_menuSync = new JMenuItem("Sync");
						_menuSync.addActionListener(this);
						this.add(_menuSync);
					}
					else
					{
						if(_majaEntry != null && _majaEntry.getType() == MajaEntry.FILE)
						{
							_menuExportFile = new JMenuItem("Export File...");
							_menuExportFile.addActionListener(this);
							this.add(_menuExportFile);
						}
						if(_majaEntry != null && _majaEntry.getType() == MajaEntry.FOLDER)
						{
							_menuExportDirectory = new JMenuItem("Export Directory...");
							_menuExportDirectory.addActionListener(this);
							this.add(_menuExportDirectory);
						}
						if(_majaEntry != null || (_majaEntry == null && _majaProject.getNumEntries(false) > 0))
						{
							_menuExportPackage = new JMenuItem("Export Package...");
							_menuExportPackage.addActionListener(this);
							this.add(_menuExportPackage);
						}
					}
				}

				if(_majaEntry != null || _majaProject != null)
				{
					this.addSeparator();

					_menuDetails = new JMenuItem("Details...");
					_menuDetails.addActionListener(this);
					this.add(_menuDetails);

					if(_majaEntry != null && _majaEntry.getType() != MajaEntry.HEAD)
					{
						_menuDelete = new JMenuItem("Remove...");
						_menuDelete.addActionListener(this);
						this.add(_menuDelete);
					}
				}
			}
			
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				if(e.getSource() == _menuNewFile)
				{
					_majaFrame.createFile(_majaProject, _majaEntry);
				}
				else if(e.getSource() == _menuNewFolder)
				{
					_majaFrame.createFolder(_majaProject, _majaEntry);
				}
				else if(e.getSource() == _menuImportFile)
				{
					_majaFrame.importFile(_majaProject, _majaEntry);
				}
				else if(e.getSource() == _menuImportDirectory)
				{
					_majaFrame.importDirectory(_majaProject, _majaEntry);
				}
				else if(e.getSource() == _menuImportPackage)
				{
					_majaFrame.importPackage(_majaProject, _majaEntry);
				}
				else if(e.getSource() == _menuRename)
				{
					//_majaFrame.renameEntry(_majaEntry);
					EntriesTree.this.startEditingAtPath(new javax.swing.tree.TreePath(_treeNode.getPath()));
				}
				else if(e.getSource() == _menuEdit)
				{
					_projectPanel.setTarget(_majaEntry);
				}
				else if(e.getSource() == _menuSync)
				{
					_majaEntry.sync();
				}
				else if(e.getSource() == _menuExportDirectory || e.getSource() == _menuExportFile)
				{
					_majaFrame.exportEntry(_majaProject, _majaEntry);
				}
				else if(e.getSource() == _menuExportPackage)
				{
					_majaFrame.exportPackage(_majaProject, _majaEntry);
				}
				else if(e.getSource() == _menuDetails)
				{
					MajaDetailsDialog dd = null;
					if(_majaEntry != null)
						dd = new MajaDetailsDialog(_majaEntry);
					else if(_majaProject != null)
						dd = new MajaDetailsDialog(_majaProject);
					
					if(dd != null)
						dd.show(_majaFrame);
				}
				else if(e.getSource() == _menuDelete)
				{
					_majaFrame.removeEntry(_majaEntry);
				}
			}
		}
	}
}