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

import org.osgi.framework.BundleContext;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectBundleInstaller;
import org.osgi.test.common.install.BundleInstaller;

public abstract class MetaTypeTest {
	@InjectBundleContext
	protected BundleContext		context;

	@InjectBundleInstaller
	protected BundleInstaller	installer;

	protected void assertAttributeBad(AttributeDefinition ad, String value) {
		assertThat(ad.validate(value).length() > 0).as("Value '" + value + "' should not have been valid").isTrue();
	}
	
	protected void assertAttributeGood(AttributeDefinition ad, String value) {
		assertThat(ad.validate(value)).as("Value '" + value + "' should have been valid").hasSize(0);
	}

	protected AttributeDefinition findAttributeDefinitionById(String id,
			ObjectClassDefinition ocd) {
		AttributeDefinition[] ads = ocd
				.getAttributeDefinitions(ObjectClassDefinition.ALL);
		if (ads == null)
			return null;
		for (int i = 0; i < ads.length; i++)
			if (ads[i].getID().equals(id))
				return ads[i];
		return null;
	}
}
