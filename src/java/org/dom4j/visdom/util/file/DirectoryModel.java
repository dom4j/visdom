/*
 * Copyright 2001 (C) MetaStuff, Ltd. All Rights Reserved.
 * 
 * This software is open source. 
 * See the bottom of this file for the licence.
 * 
 * $Id$
 */

/*
 * HACKED VERSION OF SWING PREVIEW
 *
 */
package org.dom4j.visdom.util.file;

import javax.swing.*;
import javax.swing.event.*;

import java.util.*;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import javax.swing.event.*;
import javax.swing.*;

/**
 * Models what goes on in a directory of typed files. Has a notion of
 * its current directory, and provides a notion of file typing.
 *
 * Can be told to "go up". Fakes a meta level above the root file
 * system(s) if it thinks this is a Windows box.
 *
 * @version 1.15 02/05/98
 * @author Ray Ryan
 */
public class DirectoryModel implements ListModel {
    /**
     * Cache of the TypedFiles of the local directory
     */
    transient protected Vector currentFiles = new Vector();

    /**
     * True if the <code>currentFiles</code> cache is trustworthy,
     * false if it should be refreshed
     */
    transient protected boolean currentFilesFresh = false;

    // PENDING Really should be one set of known types per thread
    // group, but then we'd be starting to turn this into a pretty
    // serious service. Needs more thought than time allows.
    /**
     * All types that are known to this file chooser. In an ideal world,
     * this would be system level information. 
     */
    protected Vector knownTypes = new Vector();

    /**
     * The type that may be shown. If null, all types shown.
     */
    protected FileType shownType = null;

    /**
     * This type is never assigned to a file. Rather, it is a general
     * rule to apply after all typing and showing is done. E.g., this
     * is how to hide all files that start with '.', regardless of
     * type.
     */
    // protected FileType hiddenRule = FileType.SharedHidden;
    protected FileType hiddenRule;

    /**
     * Special tests that must be failed before any general types
     * are assigned. Devices and such.
     */
    protected FileType[] overrideTypes = null;

    /**
     * Types to check if none of the general ones match. Last type in
     * this array must always return true for <code>testFile<code), or
     * runtime errors may result.
     */
    protected FileType[] cleanupTypes = {
	FileType.SharedFolder, FileType.SharedGenericFile };

    /**
     * Where this chooser is. Provides context for unqualified calls to
     * <code>getTypedFile</code> and  <code>getTypedFiles</code>, or calls
     * with relative paths.
     */
    protected File curDir;

    /**
     * True if we have executed our hack to see if this is a
     * Windows file system
     */
    protected boolean haveCheckedWindows = false;

    /**
     * Undefined if haveCheckedWindows is false. Otherwise, true if we
     * think this is a windows file system.
     */
    protected boolean isWindows;

    protected EventListenerList listenerList = new EventListenerList();

    protected PropertyChangeSupport changeSupport;

    /**
     * Returns a DirectoryModel whose current directory is
     * the user's home dir
     */
    public DirectoryModel() {
	this(null);
    }

    /**
     * Returns a DirectoryModel with current directory
     * <code>path</code>
     */
    public DirectoryModel(File path) {
	setCurrentDirectory(path);
	if (isWindowsFileSystem()) {
	    overrideTypes = new FileType[2];
	    overrideTypes[0] = FileType.SharedFloppyDrive;
	    overrideTypes[1] = FileType.SharedHardDrive;
	}
    }

    public void setToRootDirectory()
    {
        if ( isWindowsFileSystem() )
        {
            setCurrentDirectory( getSharedWindowsRootDir() );
        }
        else
        {
            setCurrentDirectory( new File( File.pathSeparator ) );
        }
    }

    /**
     * Convenience method, may return the same object,
     * or a new one if the give was relative.
     * If <code> f instanceof TypedFile</code>, a
     * <code>TypedFile</code> is returned.
     */
    protected File makeAbsolute(File f) {
	File dir = curDir;
	
	if (!f.isAbsolute()) {
	    if (dir == null) {
		f = f instanceof TypedFile
		? new TypedFile(f.getAbsolutePath(),
				((TypedFile)f).getType())
		: new File(f.getAbsolutePath());
	    } else {
		f = f instanceof TypedFile
		? new TypedFile(dir.getPath(), f.getPath(),
				((TypedFile)f).getType())
		: new File(dir.getPath(), f.getPath());
	    }
	}
	
	return f;
    }
    
    public File getCurrentDirectory() {
	return curDir;
    }

    /**
     * Change the directory that is the location of this model.
     * Null means go to the user's home directory
     * Emit a ChangeEvent if directory is actually changed.
     */
    public void setCurrentDirectory(File dir) {
	if (dir == null) dir = new File(System.getProperty("user.home"));

	dir = makeAbsolute(dir);

	/**
	 * Note that we compare literal paths, not canonical
	 * paths. If they want the chooser to represent the
	 * files at the end of a different route to the same
	 * place, that's perfectly legitimate. See NextStep
	 * for e.g.
	 */
	if (curDir == null || !curDir.equals(dir)) {
	    invalidateCache();
	    File oldDir = curDir;
	    curDir = dir;
	    firePropertyChange("currentDirectory", oldDir, dir);
	    fireContentsChanged();
	}
    }

    // PENDING Perhaps there should be a public version which both
    // invalidates and throws a currentDirectory property change event?
    /**
     * Invalidates the cached copy of the current directory
     */
    protected void invalidateCache() {
	currentFilesFresh = false;
    }

    /**
     * Given a path and a file name, return a TypedFile
     */
    public TypedFile getTypedFile(String path, String name) {
	TypedFile f = new TypedFile(path, name);
	deduceAndSetTypeOfFile(f);
	return f;
    }

    /**
     * Given a path, return a TypedFile
     */
    public TypedFile getTypedFile(String path) {
	TypedFile f = new TypedFile(path);
	deduceAndSetTypeOfFile(f);
	return f;
    }

    /**
     * Convenience, same as 
     * <code>getTypedFilesForDirectory(getCurrentDirectory())</code>
     */
    public Vector getTypedFiles() {
	return getTypedFilesForDirectory(getCurrentDirectory());
    }

    /**
     * Return a Vector of the files in the given directory, filtered
     * according to <code>knownTypes</code> and hiddenRule. The Vector
     * may be empty.
     *
     * The objects in the Vector are of instances of TypedFile
     */
    public Vector getTypedFilesForDirectory(File dir) {
	boolean useCache = dir.equals(getCurrentDirectory());

	if(useCache && currentFilesFresh) {
	    return currentFiles;
	} else {
	    Vector resultSet;
	    if (useCache) {
		resultSet = currentFiles;
		resultSet.removeAllElements();
	    } else {
		resultSet =  new Vector();
	    }
	    
	    String[] names = dir.list();

	    int nameCount = names == null ? 0 : names.length;
	    for (int i = 0; i < nameCount; i++) {
		TypedFile f;
		if (dir instanceof WindowsRootDir) {
		    f = getTypedFile(names[i]);
		} else {
		    f = getTypedFile(dir.getPath(), names[i]);
		}

		FileType t = f.getType();
		if ((shownType == null || t.isContainer() || shownType == t)
		    && (hiddenRule == null || !hiddenRule.testFile(f))) {
		    resultSet.addElement(f);
		}
	    }

	    // The fake windows root dir will get mangled by sorting
	    if (!(dir instanceof DirectoryModel.WindowsRootDir)) {
		sort(resultSet);
	    }

	    if (useCache) {
		currentFilesFresh = true;
	    }

	    return resultSet;
	}
    }

    /**
     * Little convenience used by <code>deduceAndSetTypeOfFile</code>
     * This is the only place <code>TypedFile.setType</code> is called
     */
    protected boolean applyTypeToFile(FileType t, TypedFile f) {
	if (t.testFile(f)) {
	    f.setType(t);
	    return true;
	}
	return false;
    }

    /**
     * Search the known types <i>et al</i> for the one that applies to
     * <code>f</code>. Must be called before <code>f.getType()</code>
     * is ever called. Do not call this twice on the same instance of
     * Typedfile, and do not call <code>f.setType()</code> after this.
     */
    protected void deduceAndSetTypeOfFile(TypedFile f) {
	// Look, a file typing loop. I am in hell.
	// We'd much rather, of course, have access to the
	// native system's file typing scheme. But we don't. Maybe
	// someday.

	boolean foundOne = false;

	// These types take precedence over everything else, for files
	// that must not be typed as anything else. They are
	// always shown. So far only faked windows drive entries
	// match this description
	//
	if (overrideTypes != null) {
	    int overrideCount = overrideTypes.length;
	    for (int j = 0; j < overrideCount && !foundOne; j++) {
		if (applyTypeToFile(overrideTypes[j], f)) {
		    foundOne = true;
		}
	    }
	}

	// Apply general types
	// 
	if (!foundOne) {
	    Enumeration e = knownTypes.elements();
	    while (e.hasMoreElements() && !foundOne) {
		FileType t = (FileType)e.nextElement();
		if (applyTypeToFile(t, f)) {
		    foundOne = true;
		}
	    }
	}

	// Cleanup. Everybody must get typed!
	// 
	if (!foundOne && cleanupTypes != null) {
	    int cleanupCount = cleanupTypes.length;
	    for (int j = 0; j < cleanupCount && !foundOne; j++) {
		if (applyTypeToFile(cleanupTypes[j], f)) {
		    foundOne = true;
		}
	    }
	}

	// Throws a run time error if no type was set
	f.getType();
    }

    //-------------------------------------------------------------------------
    // HACK - START
    //-------------------------------------------------------------------------

    public boolean canGoUp()
    {
	    File dir = getCurrentDirectory();
    	String parentPath = dir.getParent();
	    return parentPath != null
                || ( isWindowsFileSystem() && ! (dir instanceof WindowsRootDir) );
    }

    public void goUp()
    {
	File curDir = getCurrentDirectory();

	String parentPath = curDir.getParent();

    // check that we're not at "X:\." and recieved "X:\"
    if ( isWindowsFileSystem() && parentPath != null )
    {
        String temp = parentPath + ".";
        if ( temp.equals( curDir.getPath() ) )
        {
            setCurrentDirectory(getSharedWindowsRootDir());
            return;
        }
    }

    File parent = null;
    if ( parentPath != null )
    {
        if ( parentPath.charAt((int) parentPath.length()-1) == '\\'
             && isWindowsFileSystem() )
        {
            parentPath += ".";
        }
        parent = new File(parentPath);

	    setCurrentDirectory(parent);
    }

    //-------------------------------------------------------------------------
    // HACK - END
    //-------------------------------------------------------------------------

    }

    //
    // Hidden Rule
    //

    public void setHiddenRule(FileType rule) {
	if (rule != hiddenRule) {
	    FileType oldRule = hiddenRule;
	    hiddenRule = rule;
	    invalidateCache();
	    firePropertyChange("hiddenRule", oldRule, rule);
	    fireContentsChanged();
	}
    }

    public FileType getHiddenRule() {
	return hiddenRule;
    }

    //
    // Type the user wants to see now.
    //

    public FileType getShownType() {
	return shownType;
    }

    public void setShownType(FileType t) {
	shownType = t;
	if (t != null) {
	    addKnownFileType(t);
	}
	invalidateCache();
	firePropertyChange("shownType", null, null);
	fireContentsChanged();
    }

    //
    // Known File Types
    //

    public void addKnownFileType(FileType type) {
	if (type == null) {
	    throw new IllegalArgumentException("cannot add null to knownTypes");
	}

	// PENDING: should be hashing these, and checking
	// for presence via equals method. That requires changes
	// in FileType, too.
	if (!knownTypes.contains(type)) {
	    knownTypes.addElement(type);
	    invalidateCache();
	    firePropertyChange("knownFileTypes", null, null);
	    fireContentsChanged();
	}
    }

    public Enumeration enumerateKnownFileTypes() {
	return knownTypes.elements();
    }

    public Vector getKnownFileTypes() {
	return (Vector)knownTypes.clone();
    }

    public boolean isKnownFileType(FileType t) {
	// PENDING: should be hashing these, and checking
	// for presence via equals method. That requires changes
	// in FileType, too.
	return knownTypes.contains(t);
    }

    public void setKnownFileTypes(FileType[] types) {
	int count = types.length;
	Vector newTypes = knownTypes = new Vector(count);

	newTypes.setSize(count);
	for (int i = 0; i < count; i++) {
	    FileType t = types[i];
	    newTypes.setElementAt(t, i);
	}
	invalidateCache();
	firePropertyChange("knownFileTypes", null, null);
	fireContentsChanged();
    }

    //
    // Property Change Listeners
    //

    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (changeSupport != null) {
	    changeSupport.firePropertyChange(propertyName, oldValue, newValue);
	}
    }

    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport == null) {
            changeSupport = new java.beans.PropertyChangeSupport(this);
        }
        changeSupport.addPropertyChangeListener(listener);
    }

    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        if (changeSupport != null) {
	    changeSupport.removePropertyChangeListener(listener);
	}
    }
    //
    // Windows Metaroot Fakery
    //

    protected boolean isWindowsFileSystem() {
	if (!haveCheckedWindows) {
	    isWindows = false;

	    if (File.pathSeparator.equals(";")
		&& File.separator.equals("\\")) {

		String p = getCurrentDirectory().getAbsolutePath();
		if (p.length() >= 3) {
		    char zero = p.charAt(0);
		    char one = p.charAt(1);
		    char two = p.charAt(2);

		    isWindows = ((one == ':')
				 && (two == '\\')
				 && ((zero >= 'A') && (zero <= 'Z')
				     || (zero >= 'a' && zero <= 'z')));
		}
	    }

	    haveCheckedWindows = true;
	}

	return isWindows;
    }


    //-------------------------------------------------------------------------
    // HACK - START
    //-------------------------------------------------------------------------

    private static WindowsRootDir rootWindowsDir = new WindowsRootDir();

    public static WindowsRootDir getSharedWindowsRootDir()
    {
        return rootWindowsDir;

        /*
        WindowsRootDir rootDir = (WindowsRootDir)
            SwingUtilities.appContextGet(sharedWindowsRootDirKey);

	    if (rootDir == null)
        {
	        rootDir = new WindowsRootDir();
            SwingUtilities.appContextPut(sharedWindowsRootDirKey, rootDir);
	    }
	    return rootDir;
        */
    }

    //-------------------------------------------------------------------------
    // HACK - START
    //-------------------------------------------------------------------------


    /**
     * Fakes a meta level above all file systems on a suspected
     * Windows box. Its name is derived from the host name. Its
     * children are A:  and the C: through Z: devices that appear to
     * be mounted.
     * 
     * Really, "Windows-mode" should make this the desktop, where "My
     * Computer" and such appear, and similar behavior is needed for
     * Macintosh. But I don't think that's possible with Java's current
     * system and file system blindness
     * <p>
     * Warning: serialized objects of this class will not be compatible with
     * future swing releases.  The current serialization support is appropriate
     * for short term storage or RMI between Swing1.0 applications.  It will
     * not be possible to load serialized Swing1.0 objects with future releases
     * of Swing.  The JDK1.2 release of Swing will be the compatibility
     * baseline for the serialized form of Swing objects.
     */
    static public class WindowsRootDir extends TypedFile {
	protected WindowsRootDir() { super(""); }

	public boolean canRead() { return true; }
	public boolean canWrite() { return false; }
	public boolean delete() { return false; }

	public boolean equals(Object obj) { return obj == this; }
	/**
	 * Returns the hashCode for the object.  This must be defined
	 * here to ensure 100% pure.
	 *
	 * @return the hashCode for the object
	 */
	public int hashCode() {
	    return super.hashCode();
	}

	public boolean exists() { return true; }

	public String getAbsolutePath() { return getName(); }
	public String getCanonicalPath() throws IOException
	    { throw new IOException(); }

	public String getName() {
	    String hostName;

	    try {
		char name[];
		name = InetAddress.getLocalHost().getHostName().
		    toCharArray();
		name[0] = Character.toUpperCase(name[0]);
		hostName = new String(name);
	    } catch (UnknownHostException e) {
		// PENDING I18N
		hostName = "My Computer";
	    }

	    return hostName;
	}

	public String getParent() { return null; }
	public String getPath() { return getName(); }

	// public int hashCode() { return 0; }

	public boolean isAbsolute() { return true; }

	public boolean isDirectory() { return true; }
	public boolean isFile() { return false; }

	public long lastModified() { return 0; }
	public long length() { return 0; }

	public String[] list() { return list(null); }

	public String[] list(FilenameFilter filter) {
	    boolean windows = true;
            Vector deviceVector = new Vector();

            for (char c = 'C'; c <= 'Z'; c++) {
                char device[] = {c, ':', '\\', '.'};
                String devName = new String(device);
                File devFile = new File(devName);
                if (devFile != null && devFile.exists()) {
                    deviceVector.addElement(devName);
                }
            }

	    // Fake the A: floppy drive. There is no way to test if one
	    // exists or not, and we want to see it listed whether
	    // or not a floppy is "mounted". Don't even talk to me
	    // about a B: drive. Welcome to the 1990s.

	    int count = deviceVector.size() + 1;	// +1 for A:
	    String[] devices = new String[count];
	    devices[0] = "A:\\.";

	    if (count > 1) {
		for (int i = 1; i < count; i++) {
		    devices[i] = (String)deviceVector.elementAt(i-1);
		}
	    }
	    return devices;
	}

	public boolean mkdir() { return false; }
	public boolean mkdirs() { return false; }
	public boolean renameTo() { return false; }
    } // End inner class RootFile

    //
    // Sorting
    //

    // PENDING(jeff) Hopefully we can dispense with this once we
    // go to 1.2

    protected void sort(Vector v){
	quickSort(v, 0, v.size()-1);
    }

    protected boolean lt(TypedFile a, TypedFile b) {
	boolean aIsADirectory = a.getType().isContainer();
	boolean bIsADirectory = b.getType().isContainer();

	// If one is a directory and the other a file,
	// file < dir
	if (aIsADirectory != bIsADirectory) {
	    return aIsADirectory;
	} 

	return a.getName().toLowerCase().
	    compareTo(b.getName().toLowerCase()) < 0;
    }
    
    private void swap(Vector a, int i, int j) {
	Object T = a.elementAt(i); 
	a.setElementAt(a.elementAt(j), i);
	a.setElementAt(T, j);
    }

    // Liberated from the 1.1 SortDemo
    /** This is a generic version of C.A.R Hoare's Quick Sort 
    * algorithm.  This will handle arrays that are already
    * sorted, and arrays with duplicate keys.<BR>
    *
    * If you think of a one dimensional array as going from
    * the lowest index on the left to the highest index on the right
    * then the parameters to this function are lowest index or
    * left and highest index or right.  The first time you call
    * this function it will be with the parameters 0, a.length - 1.
    *
    * @param a       an integer array
    * @param lo0     left boundary of array partition
    * @param hi0     right boundary of array partition
    */
    protected void quickSort(Vector v, int lo0, int hi0) {
	int lo = lo0;
	int hi = hi0;
	TypedFile mid;

	if ( hi0 > lo0)
	{

	    /* Arbitrarily establishing partition element as the midpoint of
	     * the array.
	     */
	    mid = (TypedFile) v.elementAt(( lo0 + hi0 ) / 2);

	    // loop through the array until indices cross
	    while( lo <= hi )
	    {
		/* find the first element that is greater than or equal to 
		 * the partition element starting from the left Index.
		 */
		// Nasty to have to cast here. Would it be quicker
		// to copy the vectors into arrays and sort the arrays?
		while( ( lo < hi0 ) && lt( (TypedFile)v.elementAt(lo), mid ) )
		    ++lo;

		    /* find an element that is smaller than or equal to 
		     * the partition element starting from the right Index.
		     */
		while( ( hi > lo0 ) && lt( mid, (TypedFile)v.elementAt(hi) ) )
		    --hi;

		    // if the indexes have not crossed, swap
		if( lo <= hi ) 
		{
		    swap(v, lo, hi);
		    ++lo;
		    --hi;
		}
	    }

	    /* If the right index has not reached the left side of array
		 * must now sort the left partition.
		 */
	    if( lo0 < hi )
		quickSort( v, lo0, hi );

		/* If the left index has not reached the right side of array
		 * must now sort the right partition.
		 */
	    if( lo < hi0 )
		quickSort( v, lo, hi0 );

	}
    }


    //
    // ListModel operations
    //

    public int getSize() {
	Vector files = getTypedFiles();
	return files.size();
    }

    public Object getElementAt(int index) {
	return getTypedFiles().elementAt(index);
    }

    public void addListDataListener(ListDataListener l) {
	listenerList.add(ListDataListener.class, l);
    }

    public void removeListDataListener(ListDataListener l) {
	listenerList.remove(ListDataListener.class, l);
    }

    /*
     * Notify all listeners that have registered interest for
     * notification on this event type.  The event instance 
     * is lazily created using the parameters passed into 
     * the fire method.
     * @see EventListenerList
     */
    protected void fireContentsChanged() {
	// Guaranteed to return a non-null array
	Object[] listeners = listenerList.getListenerList();
	ListDataEvent e = null;
	// Process the listeners last to first, notifying
	// those that are interested in this event
	for (int i = listeners.length-2; i>=0; i-=2) {
	    if (listeners[i]==ListDataListener.class) {
		// Lazily create the event:
		if (e == null) {
		    e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED,
					  0, getSize()-1);
		}
		((ListDataListener)listeners[i+1]).contentsChanged(e);
	    }
	}
    }

}




/*
 * Redistribution and use of this software and associated documentation
 * ("Software"), with or without modification, are permitted provided
 * that the following conditions are met:
 *
 * 1. Redistributions of source code must retain copyright
 *    statements and notices.  Redistributions must also contain a
 *    copy of this document.
 *
 * 2. Redistributions in binary form must reproduce the
 *    above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other
 *    materials provided with the distribution.
 *
 * 3. The name "DOM4J" must not be used to endorse or promote
 *    products derived from this Software without prior written
 *    permission of MetaStuff, Ltd.  For written permission,
 *    please contact dom4j-info@metastuff.com.
 *
 * 4. Products derived from this Software may not be called "DOM4J"
 *    nor may "DOM4J" appear in their names without prior written
 *    permission of MetaStuff, Ltd. DOM4J is a registered
 *    trademark of MetaStuff, Ltd.
 *
 * 5. Due credit should be given to the DOM4J Project
 *    (http://dom4j.org/).
 *
 * THIS SOFTWARE IS PROVIDED BY METASTUFF, LTD. AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
 * METASTUFF, LTD. OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Copyright 2001 (C) MetaStuff, Ltd. All Rights Reserved.
 *
 * $Id$
 */
