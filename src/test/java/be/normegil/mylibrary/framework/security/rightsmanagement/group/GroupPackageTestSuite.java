package be.normegil.mylibrary.framework.security.rightsmanagement.group;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
		GroupTestSuite.class,
		GroupDatabaseDAOTestSuite.class
})
public class GroupPackageTestSuite {
}