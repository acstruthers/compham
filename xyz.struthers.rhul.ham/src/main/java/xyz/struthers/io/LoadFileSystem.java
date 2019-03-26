/**
 * 
 */
package xyz.struthers.io;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xyz.struthers.rhul.ham.config.Properties;

/**
 * Loads a list of files in a network folder.
 * 
 * TO DO: add in a recursive option (https://javaconceptoftheday.com/list-all-files-in-directory-in-java/)
 * 
 * @author Adam
 * @since 19-Nov-2018
 */
public class LoadFileSystem {

	/**
	 * 
	 */
	public LoadFileSystem() {
		super();
	}

	/**
	 * Gets a list of files/folders that match a given regular expression.
	 * 
	 * @param regex
	 *            The regular expression to match.
	 * @param showFolder
	 *            Include folders in search results.
	 * @param showFile
	 *            Include files in search results.
	 * @param showHidden
	 *            Include hidden files/folders in search results.
	 * @return a map containing lists of file/folder details.
	 */
	public File[] loadFileSystem(String regex, boolean showFolder, boolean showFile, boolean showHidden) {

		/*
		 * https://javaconceptoftheday.com/list-all-files-in-directory-in-java/
		 * File.listFiles(FileFilter filter) Method Example :
		 * https://docs.oracle.com/javase/8/docs/api/index.html?java/io/FileFilter.html
		 */

		File folder = new File(Properties.RESOURCE_DIRECTORY);

		FileFilter fileFilter = new FileFilter() {
			Pattern pattern = Pattern.compile(regex);

			@Override
			public boolean accept(File file) {
				boolean isMatch = true;
				Matcher matcher = pattern.matcher(file.getAbsoluteFile().toString());

				if (!matcher.find() || !showFolder && file.isDirectory() || !showFile && file.isFile()
						|| !showHidden && file.isHidden()) {
					isMatch = false;
				}
				return isMatch;
			}

		};

		return folder.listFiles(fileFilter);
	}

	/**
	 * Gets a list of all files/folders in a given network folder.
	 * 
	 * @return a List of file/folder details.
	 */
	public File[] loadFileSystem() {
		return this.loadFileSystem(".*", true, true, true); // get result with no filters
	}

	/**
	 * Gets a list of files/folders that match a given regular expression.
	 * 
	 * @param regex
	 *            The regular expression to match.
	 * @return a List of file/folder details.
	 */
	public File[] loadFileSystem(String regex) {
		// get result with regex match
		return this.loadFileSystem(regex, true, true, true);
	}

	/**
	 * Gets a list of all files/folders in a given network folder, matching a set of
	 * criteria.
	 * 
	 * @param showFolder
	 *            Include folders in search results.
	 * @param showFile
	 *            Include files in search results.
	 * @param showHidden
	 *            Include hidden files/folders in search results.
	 * @return a List of file/folder details.
	 */
	public File[] loadFileSystem(boolean showFolder, boolean showFile, boolean showHidden) {
		// get result with selected filters, matching any regex
		return this.loadFileSystem(".*", showFolder, showFile, showHidden);
	}
}
