package com.marginallyclever.robotOverlord.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.marginallyclever.robotOverlord.RobotOverlord;

/**
 * Save the world state to a file.
 * @author Admin
 *
 */
public class ActionSaveAs extends JMenuItem implements ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected RobotOverlord ro;
	
	public ActionSaveAs(RobotOverlord ro) {
		super("Save As...",KeyEvent.VK_S);
		this.ro = ro;
		addActionListener(this);
	}

	public void actionPerformed(ActionEvent e) {
		JFileChooser fc = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter("RO files", "RO");
		fc.setFileFilter(filter);
		int returnVal = fc.showSaveDialog(ro.getMainFrame());
		if (returnVal == JFileChooser.APPROVE_OPTION) {
            ro.saveWorldToFile(fc.getSelectedFile().getAbsolutePath());
		}
	}
}
