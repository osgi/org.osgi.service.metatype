/*******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
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
 *
 * SPDX-License-Identifier: Apache-2.0 
 *******************************************************************************/

package org.osgi.test.cases.metatype.junit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.namespace.extender.ExtenderNamespace;
import org.osgi.namespace.implementation.ImplementationNamespace;
import org.osgi.namespace.service.ServiceNamespace;
import org.osgi.resource.Namespace;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class CapabilitiesTestCase {

	@InjectBundleContext
	BundleContext		context;
	private static final Version							VERSION	= new Version(
			1, 4, 0);
	private static final String									PACKAGE	= "org.osgi.service.metatype";
	private static final String									NAME	= "osgi.metatype";
	private ServiceTracker<MetaTypeService, MetaTypeService>	mtsTracker;
	private BundleWiring										wiring;

	public CapabilitiesTestCase() {
	}

	@BeforeEach
	public void setUp() throws Exception {
		mtsTracker = new ServiceTracker<MetaTypeService, MetaTypeService>(context, MetaTypeService.class.getName(),
				null);
		mtsTracker.open();
		assertThat(mtsTracker.size()).as("Wrong number of MetaTypeServices").isOne();
		Bundle impl = mtsTracker.getServiceReference().getBundle();
		assertThat(impl).as("MetaTypeService implementation bundle not found").isNotNull();
		wiring = impl.adapt(BundleWiring.class);
		assertThat(wiring).as("No BundleWiring available for SCR bundle").isNotNull();
	}

	@AfterEach
	public void tearDown() throws Exception {
		mtsTracker.close();
	}

	@Test
	public void testExtenderCapability() throws Exception {
		final String namespace = ExtenderNamespace.EXTENDER_NAMESPACE;
		boolean found = false;
		List<BundleCapability> extenders = wiring.getCapabilities(namespace);
		for (BundleCapability extender : extenders) {
			if (NAME.equals(extender.getAttributes().get(namespace))) {
				found = true;
				assertThat(extender.getAttributes().get(ExtenderNamespace.CAPABILITY_VERSION_ATTRIBUTE)).as(namespace + " capability version wrong").isEqualTo(VERSION);
				String uses = extender.getDirectives().get(Namespace.CAPABILITY_USES_DIRECTIVE);
				assertThat(uses).as(namespace + " capability uses directive missing").isNotNull();
				boolean usefound = uses.indexOf(PACKAGE) >= 0;
				assertThat(usefound).as(namespace + " capability missing " + PACKAGE + " in uses").isTrue();
				break;
			}
		}
		assertThat(found).as("missing " + namespace + " capability for " + NAME).isTrue();
	}

	@Test
	public void testImplementationCapability() throws Exception {
		final String namespace = ImplementationNamespace.IMPLEMENTATION_NAMESPACE;
		boolean found = false;
		List<BundleCapability> extenders = wiring.getCapabilities(namespace);
		for (BundleCapability extender : extenders) {
			if (NAME.equals(extender.getAttributes().get(namespace))) {
				found = true;
				assertThat(extender.getAttributes().get(ImplementationNamespace.CAPABILITY_VERSION_ATTRIBUTE)).as(namespace + " capability version wrong").isEqualTo(VERSION);
				String uses = extender.getDirectives().get(Namespace.CAPABILITY_USES_DIRECTIVE);
				assertThat(uses).as(namespace + " capability uses directive missing").isNotNull();
				boolean usefound = uses.indexOf(PACKAGE) >= 0;
				assertThat(usefound).as(namespace + " capability missing " + PACKAGE + " in uses").isTrue();
				break;
			}
		}
		assertThat(found).as("missing " + namespace + " capability for " + NAME).isTrue();
	}

	@Test
	public void testServiceCapability() throws Exception {
		final String namespace = ServiceNamespace.SERVICE_NAMESPACE;
		boolean found = false;
		List<BundleCapability> services = wiring.getCapabilities(namespace);
		for (BundleCapability service : services) {
			@SuppressWarnings("unchecked")
			List<String> objectClass = (List<String>) service.getAttributes()
					.get(ServiceNamespace.CAPABILITY_OBJECTCLASS_ATTRIBUTE);
			if (objectClass.contains(MetaTypeService.class.getName())) {
				found = true;
				String uses = service.getDirectives().get(Namespace.CAPABILITY_USES_DIRECTIVE);
				assertThat(uses).as(namespace + " capability uses directive missing").isNotNull();
				boolean usefound = uses.indexOf(PACKAGE) >= 0;
				assertThat(usefound).as(namespace + " capability missing " + PACKAGE + " in uses").isTrue();
				break;
			}
		}
		assertThat(found).as("missing " + namespace + " capability for " + MetaTypeService.class.getName()).isTrue();
	}

}
