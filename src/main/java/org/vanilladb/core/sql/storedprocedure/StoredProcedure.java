/*******************************************************************************
 * Copyright 2016, 2017 vanilladb.org contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.vanilladb.core.sql.storedprocedure;

import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vanilladb.core.remote.storedprocedure.SpResultSet;
import org.vanilladb.core.server.VanillaDb;
import org.vanilladb.core.storage.tx.Transaction;
import org.vanilladb.core.storage.tx.concurrency.LockAbortException;

/**
 * An abstract class that denotes the stored procedure supported in VanillaDb.
 */
public abstract class StoredProcedure<H extends StoredProcedureParamHelper> {
	private static Logger logger = Logger.getLogger(StoredProcedure.class
			.getName());
	
	private H paramHelper;
	private Transaction tx;
	
	public StoredProcedure(H helper) {
		if (helper == null)
			throw new IllegalArgumentException("paramHelper should not be null");
		
		paramHelper = helper;
	}
	
	public void prepare(Object... pars) {
		// prepare parameters
		paramHelper.prepareParameters(pars);
		
		// create a transaction
		boolean isReadOnly = paramHelper.isReadOnly();
		tx = VanillaDb.txMgr().newTransaction(
			Connection.TRANSACTION_SERIALIZABLE, isReadOnly);
	}
	
	public SpResultSet execute() {
		try {
			executeSql();
			
			// The transaction finishes normally
			tx.commit();
			paramHelper.setCommitted(true);
			
		} catch (LockAbortException lockAbortEx) {
			if (logger.isLoggable(Level.WARNING))
				logger.warning(lockAbortEx.getMessage());
			tx.rollback();
			paramHelper.setCommitted(false);
		} catch (Exception e) {
			e.printStackTrace();
			tx.rollback();
			paramHelper.setCommitted(false);
		}

		return paramHelper.createResultSet();
	}
	
	protected abstract void executeSql();
	
	protected H getParamHelper() {
		return paramHelper;
	}
	
	protected Transaction getTransaction() {
		return tx;
	}
}