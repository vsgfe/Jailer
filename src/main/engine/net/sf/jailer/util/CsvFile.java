/*
 * Copyright 2007 - 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.jailer.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for CSV-files.
 * 
 * @author Ralf Wisser
 */
public class CsvFile {

	/**
	 * A line in a CSV-file.
	 */
	public static class Line {
		
		/**
		 * Describes the position of the line in a file.
		 */
		public final String location;
		
		/**
		 * The cells.
		 */
		public final List<String> cells;
		
		/**
		 * Length of the line.
		 */
		public int length;
		
		/**
		 * Constructor.
		 * 
		 * @param location describes the position of the line in a file
		 * @param cells the cells
		 */
		public Line(String location, List<String> cells) {
			this.location = location;
			this.cells = new ArrayList<String>(cells);
			int num = 0;
			int l = 0;
			for (String s : cells) {
				++num;
				if (s != null && s.trim().length() > 0) {
					l = num;
				}
			}
			for (int i = 0; i < 10; ++i) {
				this.cells.add("");
			}
			this.length = l;
		}

		/**
		 * Line as String.
		 */
		@Override
		public String toString() {
			int num = 0;
			int l = 0;
			for (String s : cells) {
				++num;
				if (s != null && s.trim().length() > 0) {
					l = num;
				}
			}
			StringBuffer sb = new StringBuffer();
			if (l >= cells.size()) {
				l = cells.size() - 1;
			}
			for (int i = 0; i <= l; ++i) {
				sb.append(encodeCell(cells.get(i)) + "; ");
			}
			return sb.toString();
		}
	};
	
	public static interface LineFilter {
		boolean accept(Line line);
	}
	
	/**
	 * List of lines.
	 */
	private List<Line> rows = new ArrayList<Line>();
	
	/**
	 * Indicates start of block inside a CSV file.
	 */
	public static String BLOCK_INDICATOR = "#! block ";

	/**
	 * Constructor.
	 * 
	 * @param csvFile the csv file
	 */
	public CsvFile(File csvFile) throws IOException {
		this(csvFile, null, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param csvFile the csv file
	 */
	public CsvFile(File csvFile, LineFilter filter) throws IOException {
		this(csvFile, null, filter);
	}

	/**
	 * Constructor.
	 * 
	 * @param csvFile the csv file
	 * @param block the block to read, <code>null</code> to read default block
	 */
	public CsvFile(File csvFile, String block) throws IOException {
		this(csvFile, block, null);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param csvFile the csv file
	 * @param block the block to read, <code>null</code> to read default block
	 */
	public CsvFile(File csvFile, String block, LineFilter filter) throws IOException {
		if (csvFile.exists()) {
			BufferedReader reader = new BufferedReader(new FileReader(csvFile));
			String line = null;
			int lineNr = 0;
			boolean inBlock = block == null;
			while ((line = reader.readLine()) != null) {
				++lineNr;
				if (line.trim().length() == 0) {
					continue;
				}
				if (line.trim().startsWith(BLOCK_INDICATOR)) {
					if (inBlock) {
						break;
					}
					String blockName = line.trim().substring(BLOCK_INDICATOR.length()).trim();
					inBlock = block.equals(blockName);
					continue;
				}
				if (line.trim().startsWith("#")) {
					continue;
				}
				if (!inBlock) {
					continue;
				}
				List<String> row = new ArrayList<String>();
				String[] col = decodeLine(line);
				for (int i = 0; i < col.length; ++i) {
					String s = col[i];
					row.add(s.trim());
				}
				while (row.size() < 100) {
					row.add("");
				}
				Line cvsLine = new Line("line " + lineNr + ", " + csvFile.getName(), row);
				if (filter == null || filter.accept(cvsLine)) {
					rows.add(cvsLine);
				}
			}
			reader.close();
		}
	}
	
	/**
	 * Constructor.
	 * 
	 * @param in to read from
	 * @param block the block to read, <code>null</code> to read default block
	 */
	public CsvFile(InputStream in, String block, String location, LineFilter filter) throws IOException {
		if (in != null) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String line = null;
			int lineNr = 0;
			boolean inBlock = block == null;
			while ((line = reader.readLine()) != null) {
				++lineNr;
				if (line.trim().length() == 0) {
					continue;
				}
				if (line.trim().startsWith(BLOCK_INDICATOR)) {
					if (inBlock) {
						break;
					}
					String blockName = line.trim().substring(BLOCK_INDICATOR.length()).trim();
					inBlock = block.equals(blockName);
					continue;
				}
				if (line.trim().startsWith("#")) {
					continue;
				}
				if (!inBlock) {
					continue;
				}
				List<String> row = new ArrayList<String>();
				String[] col = decodeLine(line);
				for (int i = 0; i < col.length; ++i) {
					String s = col[i];
					row.add(s.trim());
				}
				while (row.size() < 100) {
					row.add("");
				}
				Line cvsLine = new Line("line " + lineNr + ", " + location, row);
				if (filter == null || filter.accept(cvsLine)) {
					rows.add(cvsLine);
				}
			}
			in.close();
		}
	}

	/**
	 * Decodes and splits csv-line.
	 * 
	 * @param line the line to decode
	 * @return decoded and splitted line
	 */
	public static String[] decodeLine(String line) {
		List<String> cells = new ArrayList<String>(1000);
		StringBuilder sb = new StringBuilder(1000);
		boolean esc = false;
		int length = line.length();
		for (int i = 0; i < length; ++i) {
			char c = line.charAt(i);
			if (c == '\\') {
				if (esc) {
					esc = false;
				} else {
					esc = true;
					continue;
				}
			}
			if (!esc && c == ';') {
				cells.add(sb.toString());
				sb.setLength(0);
			} else {
				if (esc && c == 'n') {
					c = '\n';
				} else if (esc && c == 'r') {
					c = '\r';
				}
				sb.append(c);
			}
			esc = false;
		}
		cells.add(sb.toString());
		return cells.toArray(new String[cells.size()]);
	}

	/**
	 * Encodes and csv-cell.
	 * 
	 * @param cell the cell to encode
	 * @return encoded cell
	 */
	public static String encodeCell(String cell) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < cell.length(); ++i) {
			char c = cell.charAt(i);
			if (c == ';') {
				sb.append("\\;");
			} else if (c == '\\') {
				sb.append("\\\\");
			} else if (c == '\n') {
				sb.append("\\n");
			} else if (c == '\r') {
				sb.append("\\r");
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	/**
	 * Gets the list of lines.
	 * 
	 * @return list of lists of cell-contents
	 */
	public List<Line> getLines() {
		return rows;
	}

	/**
	 * Checks if a certain line can be found in this file.
	 * 
	 * @param the line
	 * @return <code>true</code> iff this file contains the line 
	 */
	public boolean contains(String[] line) {
		for (Line l: getLines()) {
			boolean differenceFound = false;
			int i = 0;
			for (String cell: line) {
				if (cell != null && !cell.equals(l.cells.get(i))) {
					differenceFound = true;
					break;
				}
				++i;
			}
			if (!differenceFound) {
				return true;
			}
		}
		return false;
	}
	
}
