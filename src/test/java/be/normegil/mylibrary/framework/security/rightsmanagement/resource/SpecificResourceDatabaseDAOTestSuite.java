package be.normegil.mylibrary.framework.security.rightsmanagement.resource;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
		UTSpecificResourceDatabaseDAOSafety.class,
		UTSpecificResourceDatabaseDAO.class,
		ITSpecificResourceDatabaseDAO.class
})
public class SpecificResourceDatabaseDAOTestSuite {
}