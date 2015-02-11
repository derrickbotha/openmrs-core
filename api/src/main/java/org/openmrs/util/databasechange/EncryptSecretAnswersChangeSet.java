/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.util.databasechange;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.util.Security;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * This change set is run to encrypt the users.secret_answer column
 */
public class EncryptSecretAnswersChangeSet implements CustomTaskChange {
	
	private final static Log log = LogFactory.getLog(EncryptSecretAnswersChangeSet.class);
	
	/**
	 * @see CustomTaskChange#execute(Database)
	 */
	@Override
	public void execute(Database database) throws CustomChangeException {
		JdbcConnection connection = (JdbcConnection) database.getConnection();
		Statement stmt = null;
		PreparedStatement pStmt = null;
		
		try {
			stmt = connection.createStatement();
			ResultSet rs = stmt
			        .executeQuery("SELECT user_id, salt, secret_answer FROM users WHERE secret_answer IS NOT NULL");
			pStmt = connection.prepareStatement("UPDATE users SET secret_answer = ? WHERE user_id = ?");
			while (rs.next()) {
				String answer = rs.getString("secret_answer");
				String salt = rs.getString("salt");
				String encryptedAnswer = Security.encodeString(answer.toLowerCase() + salt);
				
				pStmt.setString(1, encryptedAnswer);
				pStmt.setInt(2, rs.getInt("user_id"));
				pStmt.addBatch();
			}
			pStmt.executeBatch();
		}
		catch (DatabaseException e) {
			throw new CustomChangeException("Failed to update secret answers: " + e);
		}
		catch (SQLException e) {
			throw new CustomChangeException("Failed to update secret answers: " + e);
		}
		finally {
			if (stmt != null) {
				try {
					stmt.close();
				}
				catch (SQLException e) {
					log.warn("Failed to close the statement object");
				}
			}
			
			if (pStmt != null) {
				try {
					pStmt.close();
				}
				catch (SQLException e) {
					log.warn("Failed to close the prepared statement object");
				}
			}
		}
	}
	
	/**
	 * @see liquibase.change.custom.CustomChange#getConfirmationMessage()
	 */
	@Override
	public String getConfirmationMessage() {
		return "Finished encrypting secret answers";
	}
	
	/**
	 * @see liquibase.change.custom.CustomChange#setUp()
	 */
	@Override
	public void setUp() throws SetupException {
	}
	
	/**
	 * @see liquibase.change.custom.CustomChange#setFileOpener(liquibase.resource.ResourceAccessor)
	 */
	@Override
	public void setFileOpener(ResourceAccessor resourceAccessor) {
	}
	
	/**
	 * @see liquibase.change.custom.CustomChange#validate(liquibase.database.Database)
	 */
	@Override
	public ValidationErrors validate(Database database) {
		return null;
	}
}
