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

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;

/**
 * The 'pid' attribute of the <Designate> element within the metadata XML
 * schema is now optional like the 'factoryPid' attribute. At least one of
 * these attributes must be present, however. If both are present, an
 * implementation should ignore the 'pid' attribute.
 */
public class Bug1852Test extends MetaTypeTest {
	private final String[] FACTORY_PIDS = new String[] {
			"com.acme.factory.1",
			"com.acme.factory.2",
			"com.acme.factory.3",
			"com.acme.factory.4"
	};
	private final String[] PIDS = new String[] {
			"com.acme.singleton.1"
	};
	
	private Bundle bundle;
	private MetaTypeInformation mti;
	private ServiceReference<MetaTypeService>	ref;
	
	@Test
	public void test() {
		String[] factoryPids = mti.getFactoryPids();
		assertThat(factoryPids.length).as("Incorrect factory pids").isEqualTo(FACTORY_PIDS.length);
		assertThat(Arrays.asList(factoryPids).containsAll(Arrays.asList(FACTORY_PIDS))).as("Incorrect factory pids").isTrue();
		String[] pids = mti.getPids();
		assertThat(pids.length).as("Incorrect pids").isEqualTo(PIDS.length);
		assertThat(Arrays.asList(pids).containsAll(Arrays.asList(PIDS))).as("Incorrect pids").isTrue();
	}
	
	@BeforeEach
	public void setUp() throws Exception {
		ref = context.getServiceReference(MetaTypeService.class);
		bundle = getTestBundle();
		bundle.start();
		MetaTypeService mts = context.getService(ref);
		mti = mts.getMetaTypeInformation(bundle);
	}
	
	protected Bundle getTestBundle() throws Exception {
		return installer.installBundle("tb4.jar", false);
	}

	@AfterEach
	public void tearDown() throws Exception {
		context.ungetService(ref);
		bundle.stop();
		bundle.uninstall();
	}
}
