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
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.osgi.test.common.annotation.InjectBundleContext;
import org.osgi.test.common.annotation.InjectBundleInstaller;
import org.osgi.test.common.install.BundleInstaller;

/**
 * Get a Meta Type Service and do diverse tests.
 * 
 * @author left
 * @author $Id$
 */
public class TestControl {

	@InjectBundleContext
	BundleContext		context;

	@InjectBundleInstaller
	BundleInstaller		installer;

	String				testMethodName;

	@BeforeEach
	void testMethodName(TestInfo testInfo) {
		testMethodName = testInfo.getTestMethod().get().getName();
	}

	private Bundle			bundle;
	private ServiceReference<MetaTypeService>	ref;
	private MetaTypeService	mts;

	/**
	 * Install the test bundle
	 */
	@BeforeEach
	public void setUp() throws Exception {
		ref = context.getServiceReference(MetaTypeService.class);
		assertThat(ref).as("Service MetaTypeService is required to run this test. Install the service and try again.").isNotNull();
		mts = context.getService(ref);
		
		bundle = getTestBundle();
	}

	protected Bundle getTestBundle() throws Exception {
		return installer.installBundle("tb1.jar", false);
	}

	/**
	 * Uninstall the test bundle
	 */
	@AfterEach
	public void tearDown() throws Exception {
		context.ungetService(ref);
		bundle.uninstall();
	}

	/**
	 * Test MetaTypeService implementation
	 */
	@Test
	public void testMetaTypeService() {
		MetaTypeInformation mti;

		mti = mts.getMetaTypeInformation(bundle);
		assertThat(mti).as("MetaTypeService.getMetaTypeInformation() return an object (as expected)").isNotNull();
	}

	/**
	 * Test MetaTypeInformation implementation
	 */
	@Test
	public void testMetaTypeInformation() {
		MetaTypeInformation mti;

		mti = mts.getMetaTypeInformation(bundle);

		// Test the method getBundle()
		assertThat(mti.getBundle()).as("MetaTypeInformation.getBundle()").isSameAs(bundle);

		// Test the method getPids()
		assertThat(mti.getPids().length).as("MetaTypeInformation.getPids()").isEqualTo(3);
		assertThat(mti.getPids()[0]).as("MetaTypeInformation.getPids()[0] is not null (as expected)").isNotNull();
	}

	/**
	 * Test MetaTypeProvider implementation
	 * 
	 * @spec MetaTypeProvider.getObjectClassDefinition(String,String)
	 * @spec MetaTypeProvider.getLocales()
	 */
	@Test
	public void testMetaTypeProvider() {
		MetaTypeInformation mti;
		ObjectClassDefinition ocd;
		String[] expectedLocales;
		String[] locales;

		mti = mts.getMetaTypeInformation(bundle);

		// Test the method getObjectClassDefinition() with invalid id
		try {
			ocd = mti.getObjectClassDefinition("", "du");
			assertThat(ocd).as("MetaTypeProvider.getObjectClassDefinition() returns null (as expected)").isNull();
		}
		catch (IllegalArgumentException ex) {
			// Ignore this exception
		}

		// Test the method getObjectClassDefinition() with invalid locale
		try {
			ocd = mti.getObjectClassDefinition("com.acme.23456789", "abc");
			fail("MetaTypeProvider.getObjectClassDefinition() did not throw exception");
		}
		catch (IllegalArgumentException ex) {
			// Ignore this exception
		}

		// Test the method getObjectClassDefinition() with valid id and locale
		ocd = mti.getObjectClassDefinition("com.acme.foo", "du");
		assertThat(ocd).as("MetaTypeProvider.getObjectClassDefinition() cannot return null (as expected)").isNotNull();

		// Test the method getLocales()
		locales = mti.getLocales();
		assertThat(locales.length).as("MetaTypeProvider.getLocales()").isEqualTo(3);

		expectedLocales = new String[] {"du", "du_NL", "en_US"};
		assertThat(isArrayEquals(
				expectedLocales, locales)).as("MetaTypeProvider.getLocales()").isTrue();
	}

	/**
	 * Test ObjectClassDefinition implementation
	 * 
	 * @spec ObjectClassDefinition.getID()
	 * @spec ObjectClassDefinition.getDescription()
	 * @spec ObjectClassDefinition.getAttributeDefinitions(int)
	 * @spec ObjectClassDefinition.getIcon(int)
	 */
	@Test
	public void testObjectClassDefinition() throws Exception {
		MetaTypeInformation mti;
		ObjectClassDefinition ocd;

		mti = mts.getMetaTypeInformation(bundle);

		// Test the methods with default locale
		ocd = mti.getObjectClassDefinition("com.acme.foo", null);
		assertThat(ocd).isNotNull();
		assertThat(ocd.getID()).as("ObjectClassDefinition.getID()").isEqualTo("ocd1");
		assertThat(ocd
				.getName()).as("ObjectClassDefinition.getName()").isEqualTo("Person(default)");
		assertThat(ocd.getDescription()).as("ObjectClassDefinition.getDescription()").isEqualTo("Description(default)");
		assertThat(ocd
				.getAttributeDefinitions(ObjectClassDefinition.OPTIONAL).length).as("ObjectClassDefinition.getAttributeDefinitions()").isEqualTo(2);
		assertThat(ocd
				.getAttributeDefinitions(ObjectClassDefinition.REQUIRED).length).as("ObjectClassDefinition.getAttributeDefinitions()").isEqualTo(2);
		assertThat(ocd
				.getAttributeDefinitions(ObjectClassDefinition.ALL).length).as("ObjectClassDefinition.getAttributeDefinitions()").isEqualTo(4);
		assertThat(ocd.getIcon(32)).as("ObjectClassDefinition.getIcon()").isNotNull();
		assertThat(ocd.getIcon(16)).as("ObjectClassDefinition.getIcon()").isNotNull();

		// Test the methods with Dutch locale
		ocd = mti.getObjectClassDefinition("com.acme.foo", "du");
		assertThat(ocd.getID()).as("ObjectClassDefinition.getID()").isEqualTo("ocd1");
		assertThat(ocd
				.getName()).as("ObjectClassDefinition.getName()").isEqualTo("Persoon(du)");
		assertThat(ocd.getDescription()).as("ObjectClassDefinition.getDescription()").isEqualTo("De beschrijving");
		assertThat(ocd
				.getAttributeDefinitions(ObjectClassDefinition.OPTIONAL).length).as("ObjectClassDefinition.getAttributeDefinitions()").isEqualTo(2);
		assertThat(ocd
				.getAttributeDefinitions(ObjectClassDefinition.REQUIRED).length).as("ObjectClassDefinition.getAttributeDefinitions()").isEqualTo(2);
		assertThat(ocd
				.getAttributeDefinitions(ObjectClassDefinition.ALL).length).as("ObjectClassDefinition.getAttributeDefinitions()").isEqualTo(4);
		assertThat(ocd.getIcon(32)).as("ObjectClassDefinition.getIcon()").isNotNull();
		assertThat(ocd.getIcon(16)).as("ObjectClassDefinition.getIcon()").isNotNull();

		// Test the methods with Dutch (du_NL) locale
		ocd = mti.getObjectClassDefinition("com.acme.foo", "du_NL");
		assertThat(ocd.getID()).as("ObjectClassDefinition.getID()").isEqualTo("ocd1");
		assertThat(ocd
				.getName()).as("ObjectClassDefinition.getName()").isEqualTo("Persoon(du_NL)");
		assertThat(ocd.getDescription()).as("ObjectClassDefinition.getDescription()").isEqualTo("De beschrijving");
		assertThat(ocd
				.getAttributeDefinitions(ObjectClassDefinition.OPTIONAL).length).as("ObjectClassDefinition.getAttributeDefinitions()").isEqualTo(2);
		assertThat(ocd
				.getAttributeDefinitions(ObjectClassDefinition.REQUIRED).length).as("ObjectClassDefinition.getAttributeDefinitions()").isEqualTo(2);
		assertThat(ocd
				.getAttributeDefinitions(ObjectClassDefinition.ALL).length).as("ObjectClassDefinition.getAttributeDefinitions()").isEqualTo(4);
		assertThat(ocd.getIcon(32)).as("ObjectClassDefinition.getIcon()").isNotNull();
		assertThat(ocd.getIcon(16)).as("ObjectClassDefinition.getIcon()").isNotNull();
	}

	/**
	 * Test AttributeDefinition implementation
	 * 
	 * @spec AttributeDefinition.getID()
	 * @spec AttributeDefinition.getName()
	 * @spec AttributeDefinition.getDescription()
	 * @spec AttributeDefinition.getOptionValues()
	 * @spec AttributeDefinition.getOptionLabels()
	 * @spec AttributeDefinition.getType()
	 */
	@Test
	public void testAttributeDefinition() {
		boolean found;
		int count;
		AttributeDefinition[] attributes;
		MetaTypeInformation mti;
		ObjectClassDefinition ocd;
		String[] options;
		String[] expectedOptions;

		mti = mts.getMetaTypeInformation(bundle);
		assertThat(mti).isNotNull();

		// Get an object for tests
		ocd = mti.getObjectClassDefinition("com.acme.foo", "du");
		assertThat(ocd).isNotNull();
		attributes = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		assertThat(attributes).isNotNull();

		// Find the attribute 'sex' and run some tests
		found = false;
		for (int i = 0; i < attributes.length; i++) {
			if (attributes[i].getID().equals("sex")) {
				// Test the method testMethodName
				assertThat(attributes[i].getName()).as("AttributeDefinition.getName() is not null (as expected)").isNotNull();
				assertThat(attributes[i].getName()).as("AttributeDefinition.getName()").isEqualTo("Geslacht");

				// Test the method getDescription()
				assertThat(attributes[i].getDescription()).as("AttributeDefinition.getDescription()  is not null (as expected)").isNotNull();
				assertThat(attributes[i].getDescription()).as("AttributeDefinition.getDescription()").isEqualTo("Beschrijving");

				// Test the method getCardinality()
				assertThat(attributes[i].getCardinality()).as("AttributeDefinition.getCardinality()").isZero();

				// Test the method getType()
				assertThat(attributes[i].getType()).as("AttributeDefinition.getType()").isEqualTo(AttributeDefinition.STRING);

				// Test the method getOptionValues()
				options = attributes[i].getOptionValues();
				assertThat(options.length).as("AttributeDefinition.getOptionValues()").isEqualTo(4);

				expectedOptions = new String[] {"male", "female", "yes", "no"};
				assertThat(isArrayEquals(expectedOptions, options)).as("AttributeDefinition.getOptionValues() does not return the expected values").isTrue();

				// Test the method getOptionLabels()
				options = attributes[i].getOptionLabels();
				assertThat(options.length).as("AttributeDefinition.getOptionLabels()").isEqualTo(4);

				expectedOptions = new String[] {"Mannelijk", "Vrouwelijk",
						"Ja", "Nee"};
				assertThat(isArrayEquals(expectedOptions, options)).as("AttributeDefinition.getOptionLabels() does not return the expected values").isTrue();

				// Test the method validate
				options = new String[] {"male", "female", "yes", "no"};
				for (int j = 0; j < options.length; j++) {
					assertThat(attributes[i].validate(options[j])).as("AttributeDefinition.validate()").isEmpty();
				}
				assertThat(!(attributes[i].validate("incorrect").equals(""))).as("AttributeDefinition.validate() cannot return true with a incorrect value").isTrue();

				// Test the method getDefaultValue()
				assertThat(isArrayEquals(new String[] { "female" },
								attributes[i]
								.getDefaultValue())).as("AttributeDefinition.getDefaultValue()").isTrue();

				found = true;
			}
		}
		assertThat(found).as("Attribute with id 'sex' found (as expected)").isTrue();

		// Get other object for tests
		ocd = mti.getObjectClassDefinition("com.acme.bar", "du");
		attributes = ocd.getAttributeDefinitions(ObjectClassDefinition.ALL);
		
		// Make sure the implementation provides all of the attribute definitions 
		// specified within the metadata XML (testfile.xml - ocd2).
		assertThat(attributes.length).as("Missing one or more attribute definitions").isEqualTo(10);

		// Execute attribute type tests
		count = 0;
		for (int i = 0; i < attributes.length; i++) {
			if (attributes[i].getID().equals("boolean")) {
				count++;
				assertThat(attributes[i].getType()).as("A boolean attribute must be of type AttributeDefinition.BOOLEAN").isEqualTo(AttributeDefinition.BOOLEAN);
			}
			else
				if (attributes[i].getID().equals("byte")) {
					count++;
					assertThat(attributes[i].getType()).as("A byte attribute must be of type AttributeDefinition.BYTE").isEqualTo(AttributeDefinition.BYTE);
				}
				else
					if (attributes[i].getID().equals("character")) {
						count++;
						assertThat(attributes[i]
										.getType()).as("A character attribute must be of type AttributeDefinition.CHARACTER").isEqualTo(AttributeDefinition.CHARACTER);
					}
					else
						if (attributes[i].getID().equals("double")) {
							count++;
							assertThat(attributes[i]
											.getType()).as("A double attribute must be of type AttributeDefinition.DOUBLE").isEqualTo(AttributeDefinition.DOUBLE);
						}
						else
							if (attributes[i].getID().equals("float")) {
								count++;
								assertThat(attributes[i].getType()).as("A float attribute must be of type AttributeDefinition.FLOAT").isEqualTo(AttributeDefinition.FLOAT);
							}
							else
								if (attributes[i].getID().equals("integer")) {
									count++;
									assertThat(attributes[i].getType()).as("A integer attribute must be of type AttributeDefinition.INTEGER").isEqualTo(AttributeDefinition.INTEGER);
								}
								else
									if (attributes[i].getID().equals("long")) {
										count++;
										assertThat(attributes[i].getType()).as("A long attribute must be of type AttributeDefinition.LONG").isEqualTo(AttributeDefinition.LONG);
									}
									else
										if (attributes[i].getID().equals(
												"short")) {
											count++;
											assertThat(attributes[i].getType()).as("A short attribute must be of type AttributeDefinition.SHORT").isEqualTo(AttributeDefinition.SHORT);
										}
										else
											if (attributes[i].getID().equals(
													"string")) {
												count++;
												assertThat(attributes[i].getType()).as("A string attribute must be of type AttributeDefinition.STRING").isEqualTo(AttributeDefinition.STRING);
											}
											else
												if (attributes[i].getID().equals("password")) {
													count++;
													assertThat(attributes[i].getType()).as("A password attribute must be of type AttributeDefinition.PASSWORD").isEqualTo(AttributeDefinition.PASSWORD);
												}
		}

		assertThat(count).as("All types must be tested").isEqualTo(attributes.length);
	}

	/**
	 * Test MetaTypeService when the bundle has an own implementation of MetaTypeProvider
	 * 
	 * @spec MetaTypeInformation.getPids()
	 * @spec MetaTypeInformation.getObjectClassDefinition(String,String)
	 */
	@Test
	public void testBundleMetaTypeProvider() throws Exception {
		Bundle tb2;
		MetaTypeInformation mti;
		ServiceReference<MetaTypeProvider> sr;

		tb2 = installer.installBundle("tb2.jar");
		tb2.start();

		// Check if the service is registered
		sr = context.getServiceReference(
				MetaTypeProvider.class);
		assertThat(sr).as("Checking if the service is registered").isNotNull();

		// Get an object for tests
		mti = mts.getMetaTypeInformation(tb2);
		assertThat(mti.getPids().length).as("Checking the number of PIDs").isOne();

		tb2.stop();
		tb2.uninstall();
	}
	
	/**
	 * Tests a MetaTypeService when a bundle has provided its own implementation
	 * of a MetaTypeProvider that is registered as a service and is neither a 
	 * ManagedService nor ManagedServiceFactory. METATYPE_PID (String) only.
	 * 
	 * @spec MetaTypeService.getMetaTypeInformation(Bundle)
	 * @spec MetaTypeInformation.getPids()
	 * @spec MetaTypeInformation.getFactoryPids()
	 */
	@Test
	public void testBundleMetaTypeProvider1() throws Exception {
		BundleContext bundleContext = context;
		Dictionary<String,Object> properties = new Hashtable<>();
		String servicePid = "testBundleMetaTypeProvider1";
		properties.put(
				Constants.SERVICE_PID, 
				servicePid);
		String metatypePid = "testBundleMetaTypeProvider1.1";
		properties.put(
				MetaTypeProvider.METATYPE_PID, 
				metatypePid);
		ServiceRegistration<MetaTypeProvider> serviceRegistration = bundleContext
				.registerService(MetaTypeProvider.class,
				new MetaTypeProvider() {
					public ObjectClassDefinition getObjectClassDefinition(
							String id, String locale) {
						return null;
					}

					public String[] getLocales() {
						return null;
					}
				}, 
				properties);
		MetaTypeInformation mti = mts.getMetaTypeInformation(bundleContext.getBundle());
		assertThat(mti).as("The returned MetaTypeInformation was null").isNotNull();
		assertThat(isArrayEquals(new String[]{metatypePid}, mti.getPids())).as("The pids returned by MetaTypeInformation did not match the METATYPE_PID property").isTrue();
		assertThat(mti.getFactoryPids() == null || mti.getFactoryPids().length == 0).as("The factory pid array returned by MetaTypeInformation was not null or an empty array").isTrue();
		serviceRegistration.unregister();
	}
	
	/**
	 * Tests a MetaTypeService when a bundle has provided its own implementation
	 * of a MetaTypeProvider that is registered as a service and is neither a 
	 * ManagedService nor ManagedServiceFactory. METATYPE_FACTORY_PID 
	 * (Collection<String>) only.
	 * 
	 * @spec MetaTypeService.getMetaTypeInformation(Bundle)
	 * @spec MetaTypeInformation.getPids()
	 * @spec MetaTypeInformation.getFactoryPids()
	 */
	@Test
	public void testBundleMetaTypeProvider2() throws Exception {
		BundleContext bundleContext = context;
		Dictionary<String,Object> properties = new Hashtable<>();
		String servicePid = "testBundleMetaTypeProvider2";
		properties.put(
				Constants.SERVICE_PID, 
				servicePid);
		Collection<String> metatypeFactoryPids = new ArrayList<>();
		metatypeFactoryPids.add("testBundleMetaTypeProvider2.1");
		metatypeFactoryPids.add("testBundleMetaTypeProvider2.2");
		metatypeFactoryPids.add("testBundleMetaTypeProvider2.3");
		properties.put(
				MetaTypeProvider.METATYPE_FACTORY_PID, 
				metatypeFactoryPids);
		ServiceRegistration<MetaTypeProvider> serviceRegistration = bundleContext
				.registerService(MetaTypeProvider.class,
				new MetaTypeProvider() {
					public ObjectClassDefinition getObjectClassDefinition(
							String id, String locale) {
						return null;
					}

					public String[] getLocales() {
						return null;
					}
				}, 
				properties);
		MetaTypeInformation mti = mts.getMetaTypeInformation(bundleContext.getBundle());
		assertThat(mti).as("The returned MetaTypeInformation was null").isNotNull();
		assertThat(isArrayEquals(metatypeFactoryPids.toArray(new String[metatypeFactoryPids.size()]), mti.getFactoryPids())).as("The factory pids returned by MetaTypeInformation did not match the METATYPE_FACTORY_PID property").isTrue();
		assertThat(mti.getPids() == null || mti.getPids().length == 0).as("The pid array returned by MetaTypeInformation was not null or an empty array").isTrue();
		serviceRegistration.unregister();
	}
	
	/**
	 * Tests a MetaTypeService when a bundle has provided its own implementation
	 * of a MetaTypeProvider that is registered as a service and is neither a 
	 * ManagedService nor ManagedServiceFactory. METATYPE_PID (String[]) and
	 * METATYPE_FACTORY_PID (Collection<String>).
	 * 
	 * @spec MetaTypeService.getMetaTypeInformation(Bundle)
	 * @spec MetaTypeInformation.getPids()
	 * @spec MetaTypeInformation.getFactoryPids()
	 */
	@Test
	public void testBundleMetaTypeProvider3() throws Exception {
		BundleContext bundleContext = context;
		Dictionary<String,Object> properties = new Hashtable<>();
		String servicePid = "testBundleMetaTypeProvider3";
		properties.put(
				Constants.SERVICE_PID, 
				servicePid);
		String[] metatypePids = new String[] {
				"testBundleMetaTypeProvider3.1",
				"testBundleMetaTypeProvider3.2"
		};
		Collection<String> metatypeFactoryPids = new ArrayList<>();
		metatypeFactoryPids.add("testBundleMetaTypeProvider3.3");
		metatypeFactoryPids.add("testBundleMetaTypeProvider3.4");
		properties.put(
				MetaTypeProvider.METATYPE_PID, 
				metatypePids);
		properties.put(
				MetaTypeProvider.METATYPE_FACTORY_PID, 
				metatypeFactoryPids);
		ServiceRegistration<MetaTypeProvider> serviceRegistration = bundleContext
				.registerService(MetaTypeProvider.class,
				new MetaTypeProvider() {
					public ObjectClassDefinition getObjectClassDefinition(
							String id, String locale) {
						return null;
					}

					public String[] getLocales() {
						return null;
					}
				}, 
				properties);
		MetaTypeInformation mti = mts.getMetaTypeInformation(bundleContext.getBundle());
		assertThat(mti).as("The returned MetaTypeInformation was null").isNotNull();
		assertThat(isArrayEquals(metatypePids, mti.getPids())).as("The pids returned by MetaTypeInformation did not match the METATYPE_PID property").isTrue();
		assertThat(isArrayEquals(metatypeFactoryPids.toArray(new String[metatypeFactoryPids.size()]), mti.getFactoryPids())).as("The factory pids returned by MetaTypeInformation did not match the METATYPE_FACTORY_PID property").isTrue();
		serviceRegistration.unregister();
	}
	
	/**
	 * Tests a MetaTypeService when a bundle has provided its own implementation
	 * of a MetaTypeProvider that is registered as a service and also as a 
	 * ManagedService. SERVICE_PID (String), METATYPE_PID (Collection<String>) 
	 * and METATYPE_FACTORY_PID (String[]).
	 * 
	 * @spec MetaTypeService.getMetaTypeInformation(Bundle)
	 * @spec MetaTypeInformation.getPids()
	 * @spec MetaTypeInformation.getFactoryPids()
	 */
	@Test
	public void testBundleMetaTypeProvider4() throws Exception {
		BundleContext bundleContext = context;
		Dictionary<String,Object> properties = new Hashtable<>();
		String servicePid = "testBundleMetaTypeProvider4";
		Collection<String> metatypePids = new ArrayList<>();
		metatypePids.add("testBundleMetaTypeProvider4.1");
		metatypePids.add("testBundleMetaTypeProvider4.2");
		metatypePids.add("testBundleMetaTypeProvider4.3");
		String[] metatypeFactoryPids = new String[] {
				"testBundleMetaTypeProvider4.5",
				"testBundleMetaTypeProvider4.6"
		};
		properties.put(
				Constants.SERVICE_PID, 
				servicePid);
		properties.put(
				MetaTypeProvider.METATYPE_PID, 
				metatypePids);
		properties.put(
				MetaTypeProvider.METATYPE_FACTORY_PID, 
				metatypeFactoryPids);
		ManagedServiceMetaTypeProvider service = new ManagedServiceMetaTypeProvider();
		ServiceRegistration< ? > serviceRegistration = bundleContext
				.registerService(
				new String[] {
						ManagedService.class.getName(),
						MetaTypeProvider.class.getName()}, 
				service, 
				properties);
		MetaTypeInformation mti = mts.getMetaTypeInformation(bundleContext.getBundle());
		assertThat(mti).as("The returned MetaTypeInformation was null").isNotNull();
		Collection<String> pids = new ArrayList<>(metatypePids);
		pids.add(servicePid);
		assertThat(isArrayEquals(pids.toArray(new String[pids.size()]), mti.getPids())).as("The pids returned by MetaTypeInformation did not match the SERVICE_PID and METATYPE_PID properties").isTrue();
		assertThat(isArrayEquals(metatypeFactoryPids, mti.getFactoryPids())).as("The factory pids returned by MetaTypeInformation did not match the METATYPE_FACTORY_PID property").isTrue();
		serviceRegistration.unregister();
	}
	
	/**
	 * Tests a MetaTypeService when a bundle has provided its own implementation
	 * of a MetaTypeProvider that is registered as a service and also as a 
	 * ManagedServiceFactory. SERVICE_PID (Collection<String>), METATYPE_PID 
	 * (Collection<String>) and METATYPE_FACTORY_PID (String).
	 * 
	 * @spec MetaTypeService.getMetaTypeInformation(Bundle)
	 * @spec MetaTypeInformation.getPids()
	 * @spec MetaTypeInformation.getFactoryPids()
	 */
	@Test
	public void testBundleMetaTypeProvider5() throws Exception {
		BundleContext bundleContext = context;
		Dictionary<String,Object> properties = new Hashtable<>();
		Collection<String> servicePids = new ArrayList<>();
		servicePids.add("testBundleMetaTypeProvider5.1");
		servicePids.add("testBundleMetaTypeProvider5.2");
		Collection<String> metatypePids = new ArrayList<>();
		metatypePids.add("testBundleMetaTypeProvider5.3");
		metatypePids.add("testBundleMetaTypeProvider5.4");
		metatypePids.add("testBundleMetaTypeProvider5.5");
		String metatypeFactoryPid = "testBundleMetaTypeProvider5.6";
		properties.put(
				Constants.SERVICE_PID, 
				servicePids);
		properties.put(
				MetaTypeProvider.METATYPE_PID, 
				metatypePids);
		properties.put(
				MetaTypeProvider.METATYPE_FACTORY_PID, 
				metatypeFactoryPid);
		ManagedServiceFactoryMetaTypeProvider service = new ManagedServiceFactoryMetaTypeProvider();
		ServiceRegistration< ? > serviceRegistration = bundleContext
				.registerService(
				new String[] {
						ManagedServiceFactory.class.getName(),
						MetaTypeProvider.class.getName()}, 
				service, 
				properties);
		MetaTypeInformation mti = mts.getMetaTypeInformation(bundleContext.getBundle());
		assertThat(mti).as("The returned MetaTypeInformation was null").isNotNull();
		assertThat(isArrayEquals(metatypePids.toArray(new String[metatypePids.size()]), mti.getPids())).as("The pids returned by MetaTypeInformation did not match the METATYPE_PID property").isTrue();
		Collection<String> factoryPids = new ArrayList<>(servicePids);
		factoryPids.add(metatypeFactoryPid);
		assertThat(isArrayEquals(factoryPids.toArray(new String[factoryPids.size()]), mti.getFactoryPids())).as("The factory pids returned by MetaTypeInformation did not match the METATYPE_FACTORY_PID property").isTrue();
		serviceRegistration.unregister();
	}
	
	/**
	 * Tests a MetaTypeService when a bundle has registered more than one
	 * MetaTypeProvider service, none of which are also registered as a
	 * ManagedService or ManagedServiceFactory.
	 * 
	 * @spec MetaTypeService.getMetaTypeInformation(Bundle)
	 * @spec MetaTypeInformation.getPids()
	 * @spec MetaTypeInformation.getFactoryPids()
	 */
	@Test
	public void testBundleMetaTypeProvider6() throws Exception {
		BundleContext bundleContext = context;
		Dictionary<String,Object> properties = new Hashtable<>();
		String servicePid = "testBundleMetaTypeProvider6";
		properties.put(
				Constants.SERVICE_PID, 
				servicePid);
		Collection<String> metatype1pids = new ArrayList<>();
		metatype1pids.add("testBundleMetaTypeProvider6.1");
		metatype1pids.add("testBundleMetaTypeProvider6.2");
		Collection<String> metatype1factoryPids = new ArrayList<>();
		metatype1factoryPids.add("testBundleMetaTypeProvider6.3");
		metatype1factoryPids.add("testBundleMetaTypeProvider6.4");
		metatype1factoryPids.add("testBundleMetaTypeProvider6.5");
		properties.put(
				MetaTypeProvider.METATYPE_PID, 
				metatype1pids);
		properties.put(
				MetaTypeProvider.METATYPE_FACTORY_PID, 
				metatype1factoryPids);
		ServiceRegistration<MetaTypeProvider> serviceRegistration1 = bundleContext
				.registerService(MetaTypeProvider.class,
				new MetaTypeProvider() {
					public ObjectClassDefinition getObjectClassDefinition(
							String id, String locale) {
						return null;
					}

					public String[] getLocales() {
						return null;
					}
				}, 
				properties);
		Collection<String> metatype2pids = new ArrayList<>();
		metatype2pids.add("testBundleMetaTypeProvider6.6");
		metatype2pids.add("testBundleMetaTypeProvider6.7");
		metatype2pids.add("testBundleMetaTypeProvider6.8");
		Collection<String> metatype2factoryPids = new ArrayList<>();
		metatype2factoryPids.add("testBundleMetaTypeProvider6.9");
		metatype2factoryPids.add("testBundleMetaTypeProvider6.10");
		properties = new Hashtable<>();
		properties.put(
				MetaTypeProvider.METATYPE_PID, 
				metatype2pids);
		properties.put(
				MetaTypeProvider.METATYPE_FACTORY_PID, 
				metatype2factoryPids);
		ServiceRegistration<MetaTypeProvider> serviceRegistration2 = bundleContext
				.registerService(MetaTypeProvider.class,
				new MetaTypeProvider() {
					public ObjectClassDefinition getObjectClassDefinition(
							String id, String locale) {
						return null;
					}

					public String[] getLocales() {
						return null;
					}
				}, 
				properties);
		MetaTypeInformation mti = mts.getMetaTypeInformation(bundleContext.getBundle());
		assertThat(mti).as("The returned MetaTypeInformation was null").isNotNull();
		Collection<String> pids = new ArrayList<>(metatype1pids);
		pids.addAll(metatype2pids);
		assertThat(isArrayEquals(pids.toArray(new String[pids.size()]), mti.getPids())).as("The pids returned by MetaTypeInformation did not match the METATYPE_PID properties from both MetaTypeProviders").isTrue();
		Collection<String> factoryPids = new ArrayList<>(metatype1factoryPids);
		factoryPids.addAll(metatype2factoryPids);
		assertThat(isArrayEquals(factoryPids.toArray(new String[factoryPids.size()]), mti.getFactoryPids())).as("The factory pids returned by MetaTypeInformation did not match the METATYPE_FACTORY_PID properties from both MetaTypeProviders").isTrue();
		serviceRegistration2.unregister();
		serviceRegistration1.unregister();
	}

	/**
	 * Compare two arrays in an elements order independent manner.
	 * 
	 * @param _array1 The first array to compare
	 * @param _array2 The second array to compare
	 * @return <code>true</code> If the two arrays have the same elements
	 *         <code>false</code> If the two arrays do not have the same
	 *         elements
	 */
	private boolean isArrayEquals(Object[] _array1, Object[] _array2) {
		boolean result;
		Collection<Object> collection;

		result = (_array1.length == _array2.length);

		if (result) {
			collection = Arrays.asList(_array1);

			for (int i = 0; i < _array2.length && result; i++) {
				result = collection.contains(_array2[i]);
			}
		}

		return result;
	}

	static class ManagedServiceMetaTypeProvider implements ManagedService,
			MetaTypeProvider {
		public ObjectClassDefinition getObjectClassDefinition(String id,
				String locale) {
			return null;
		}

		public String[] getLocales() {
			return null;
		}

		public void updated(Dictionary<String, ? > properties)
				throws ConfigurationException {
			// noop
		}
	}
	
	static class ManagedServiceFactoryMetaTypeProvider implements
			ManagedServiceFactory, MetaTypeProvider {
		public ObjectClassDefinition getObjectClassDefinition(String id,
				String locale) {
			return null;
		}

		public String[] getLocales() {
			return null;
		}

		public String getName() {
			return null;
		}

		public void updated(String pid, Dictionary<String, ? > properties)
				throws ConfigurationException {
			// noop
		}

		public void deleted(String pid) {
			// noop
		}
	}
}
