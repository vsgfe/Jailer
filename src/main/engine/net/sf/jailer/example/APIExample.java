/*
 * Copyright 2007 - 2017 the original author or authors.
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
package net.sf.jailer.example;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import net.sf.jailer.api.Importer;
import net.sf.jailer.api.Subsetter;
import net.sf.jailer.configuration.Configuration;
import net.sf.jailer.database.BasicDataSource;
import net.sf.jailer.subsetting.ScriptFormat;

public class APIExample {
	
	private static final int POOL_SIZE = 10;

	private static Subsetter subsetter = 
		new Subsetter(
			new BasicDataSource(
					"org.h2.Driver", "jdbc:h2:demo-scott", "sa", "",
					POOL_SIZE,
					new File("lib/h2-1.3.160.jar")),
			null,
			APIExample.class.getResource("Demo-Scott"),
			APIExample.class.getResource("Demo-Scott.csv"),
			ScriptFormat.SQL);
	
	private static Importer importer =
		new Importer(
			new BasicDataSource(
				"org.h2.Driver", "jdbc:h2:demo-scott-subset", "sa", "",
				10,
				new File("lib/h2-1.3.160.jar")));

	public static void main(String[] args) throws SQLException, IOException {
		new APIExample().exportScott();
		
		long t = System.currentTimeMillis();

		for (int i = 0; i < 100; ++i) {
			new APIExample().exportScott();
		}
		
		System.out.println(System.currentTimeMillis() - t);
		
	}

	private void exportScott() throws SQLException, IOException {
		File exportScriptFile = Configuration.getInstance().createTempFile();
		
		subsetter.setWhereClause("NAME='SCOTT'");
		subsetter.setUpsertOnly(true); // overwrite previous data
		subsetter.execute(exportScriptFile);
		
		importer.execute(exportScriptFile);
		
		exportScriptFile.delete();
	}

}
