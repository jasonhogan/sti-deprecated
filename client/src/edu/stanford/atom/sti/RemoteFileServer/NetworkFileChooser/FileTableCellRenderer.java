/** @file FileTableCellRenderer.java
 *  @author Jason Michael Hogan
 *  @brief Source-file for the class FileTableCellRenderer
 *  @section license License
 *
 *  Copyright (C) 2008 Jason Hogan <hogan@stanford.edu>\n
 *  This file is part of the Stanford Timing Interface (STI).
 *
 *  The STI is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  The STI is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with the STI.  If not, see <http://www.gnu.org/licenses/>.
 */

package edu.stanford.atom.sti.RemoteFileServer.NetworkFileChooser;

import javax.swing.*;
import javax.swing.table.*;
import edu.stanford.atom.sti.RemoteFileServer.comm.corba.*;

public class FileTableCellRenderer extends DefaultTableCellRenderer  {

    Icon fileIcon = UIManager.getIcon("FileView.fileIcon");
    Icon directoryIcon = UIManager.getIcon("FileView.directoryIcon");
     
    @Override
    protected void setValue(Object value) {
        if(value instanceof TFile) {
            TFile tFile = (TFile)value;
            setText(tFile.filename);
            if(tFile.isDirectory) {
                setIcon(directoryIcon);
            }
            else {
                setIcon(fileIcon);
            }
        }
    }
}
