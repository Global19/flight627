/*******************************************************************************
 *  Copyright (c) 2013 GoPivotal, Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.flight.core;

import org.eclipse.core.resources.IProject;

/**
 * @author Martin Lippert
 */
public interface IRepositoryListener {
	
	void projectConnected(IProject project);
	void projectDisconnected(IProject project);

}
