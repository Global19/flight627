/*******************************************************************************
 * Copyright (c) 2013 Pivotal Software, Inc. and others.
 * All rights reserved. This program and the accompanying materials are made 
 * available under the terms of the Eclipse Public License v1.0 
 * (http://www.eclipse.org/legal/epl-v10.html), and the Eclipse Distribution 
 * License v1.0 (http://www.eclipse.org/org/documents/edl-v10.html). 
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
*******************************************************************************/
package org.eclipse.flight.jdt.services;

import org.eclipse.core.resources.IResource;
import org.eclipse.flight.core.AbstractMessageHandler;
import org.eclipse.flight.core.IMessageHandler;
import org.eclipse.flight.core.IMessagingConnector;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Martin Lippert
 */
public class NavigationService {

	private LiveEditUnits liveEditUnits;
	private IMessagingConnector messagingConnector;

	public NavigationService(IMessagingConnector messagingConnector, LiveEditUnits liveEditUnits) {
		this.messagingConnector = messagingConnector;
		this.liveEditUnits = liveEditUnits;
		
		IMessageHandler contentAssistRequestHandler = new AbstractMessageHandler("navigationrequest") {
			@Override
			public void handleMessage(String messageType, JSONObject message) {
				handleNavigationRequest(message);
			}
		};
		messagingConnector.addMessageHandler(contentAssistRequestHandler);
	}
	
	protected void handleNavigationRequest(JSONObject message) {
		try {
			String projectName = message.getString("project");
			String resourcePath = message.getString("resource");
			int callbackID = message.getInt("callback_id");
			
			String liveEditID = projectName + "/" + resourcePath;
			if (liveEditUnits.isLiveEditResource(liveEditID)) {

				int offset = message.getInt("offset");
				int length = message.getInt("length");
				String sender = message.getString("requestSenderID");

				JSONObject navigationResult = computeNavigation(liveEditID, offset, length);
				
				if (navigationResult != null) {
					JSONObject responseMessage = new JSONObject();
					responseMessage.put("project", projectName);
					responseMessage.put("resource", resourcePath);
					responseMessage.put("callback_id", callbackID);
					responseMessage.put("requestSenderID", sender);
					responseMessage.put("navigation", navigationResult);
	
					messagingConnector.send("navigationresponse", responseMessage);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public JSONObject computeNavigation(String requestorResourcePath, int offset, int length) {
		try {
			ICompilationUnit liveEditUnit = liveEditUnits.getLiveEditUnit(requestorResourcePath);
			if (liveEditUnit != null) {
				IJavaElement[] elements = liveEditUnit.codeSelect(offset, length);
	
				if (elements != null && elements.length > 0) {
					JSONObject result = new JSONObject();
					
					IJavaElement element = elements[0];
					IResource resource = element.getResource();
					
					if (resource != null && resource.getProject() != null) {
						String projectName = resource.getProject().getName();
						String resourcePath = resource.getProjectRelativePath().toString();
						
						result.put("project", projectName);
						result.put("resource", resourcePath);
		
						if (element instanceof ISourceReference) {
							ISourceRange nameRange = ((ISourceReference) element).getNameRange();
							result.put("offset", nameRange.getOffset());
							result.put("length", nameRange.getLength());
						}
						
						return result;
					}
					else {
						while (element != null && !(element instanceof IClassFile)) {
							element = element.getParent();
						}
						
						if (element != null && element instanceof IClassFile) {
							IClassFile classFile = (IClassFile) element;
							ISourceRange sourceRange = classFile.getSourceRange();
							if (sourceRange != null) {
								String projectName = element.getJavaProject().getProject().getName();
								String resourcePath  = classFile.getParent().getElementName().replace('.', '/');
								resourcePath = "classpath:/" + resourcePath + "/" + classFile.getElementName();
								
								result.put("project", projectName);
								result.put("resource", resourcePath);
								
								return result;
							}
						}
					}
				}
			}

		} catch (JavaModelException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

}
