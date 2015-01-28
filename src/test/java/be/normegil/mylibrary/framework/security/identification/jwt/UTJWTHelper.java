package be.normegil.mylibrary.framework.security.identification.jwt;

import be.normegil.mylibrary.ApplicationProperties;
import be.normegil.mylibrary.framework.DateHelper;
import be.normegil.mylibrary.framework.security.identification.key.KeyManager;
import be.normegil.mylibrary.framework.security.identification.key.KeyType;
import be.normegil.mylibrary.tools.ClassWrapper;
import be.normegil.mylibrary.tools.FieldWrapper;
import be.normegil.mylibrary.tools.GeneratorRepository;
import be.normegil.mylibrary.tools.IGenerator;
import be.normegil.mylibrary.tools.dao.KeyMemoryDAO;
import be.normegil.mylibrary.user.User;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.ReadOnlyJWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.junit.Assert.*;

public class UTJWTHelper {

	private static final IGenerator<User> GENERATOR = GeneratorRepository.get(User.class);
	private static final LocalDateTime DEFAULT_TIME = LocalDateTime.of(2015, Month.OCTOBER, 26, 11, 50, 32);
	public static final LocalDateTime DEFAULT_VALIDITY_DATE = DEFAULT_TIME.plus(ApplicationProperties.Security.JSonWebToken.TOKEN_VALIDITY_PERIOD);
	private JWTHelper jwtHelper;
	private KeyManager keyManager;

	@Before
	public void setUp() throws Exception {
		jwtHelper = new JWTHelper() {
			@Override
			protected LocalDateTime getCurrentTime() {
				return DEFAULT_TIME;
			}
		};
		ClassWrapper<JWTHelper> jwtHelperClass = new ClassWrapper<>(JWTHelper.class);
		FieldWrapper field = jwtHelperClass.getField("keyManager");

		keyManager = new KeyManager(new KeyMemoryDAO());
		field.set(jwtHelper, keyManager);
	}

	@After
	public void tearDown() throws Exception {
		jwtHelper = null;
		keyManager = null;
	}

	@Test
	public void testGenerateSignedJWT_Validity() throws Exception {
		User user = GENERATOR.getDefault(true, true);
		assertTrue(jwtHelper.isValid(jwtHelper.generateSignedJWT(user)));
	}

	@Test
	public void testGenerateSignedJWT_PropertiesAndHeader() throws Exception {
		User user = GENERATOR.getDefault(true, true);
		KeyPair keyPair = keyManager.load(JWTHelper.JWT_SIGNING_KEY_NAME, KeyType.ECDSA);
		SignedJWT jwt = getSignedJWT(user, keyPair, DEFAULT_TIME, DEFAULT_VALIDITY_DATE);
		assertJWTEquals(jwt, jwtHelper.generateSignedJWT(user));
	}

	@Test
	public void testIsValid_ValidJWT() throws Exception {
		User user = GENERATOR.getDefault(true, true);
		KeyPair keyPair = keyManager.load(JWTHelper.JWT_SIGNING_KEY_NAME, KeyType.ECDSA);
		SignedJWT signedJWT = getSignedJWT(user, keyPair, DEFAULT_TIME, DEFAULT_VALIDITY_DATE);
		assertTrue(jwtHelper.isValid(signedJWT));
	}

	@Test
	public void testIsValid_WrongSigningKeys() throws Exception {
		User user = GENERATOR.getDefault(true, true);
		KeyPair keyPair = keyManager.load("FakeKeys", KeyType.ECDSA);
		SignedJWT signedJWT = getSignedJWT(user, keyPair, DEFAULT_TIME, DEFAULT_VALIDITY_DATE);
		assertFalse(jwtHelper.isValid(signedJWT));
	}

	@Test
	public void testIsValid_OutdatedJWT() throws Exception {
		User user = GENERATOR.getDefault(true, true);
		KeyPair keyPair = keyManager.load(JWTHelper.JWT_SIGNING_KEY_NAME, KeyType.ECDSA);
		SignedJWT signedJWT = getSignedJWT(user, keyPair, DEFAULT_TIME, LocalDateTime.now().minus(5, ChronoUnit.YEARS));
		assertFalse(jwtHelper.isValid(signedJWT));
	}

	@Test
	public void testIsValid_JWTInFuture() throws Exception {
		User user = GENERATOR.getDefault(true, true);
		KeyPair keyPair = keyManager.load(JWTHelper.JWT_SIGNING_KEY_NAME, KeyType.ECDSA);
		SignedJWT signedJWT = getSignedJWT(user, keyPair, LocalDateTime.now().plus(5, ChronoUnit.YEARS), DEFAULT_VALIDITY_DATE);
		assertFalse(jwtHelper.isValid(signedJWT));
	}

	@Test
	public void testDefaultCurrentTime() throws Exception {
		assertEquals(LocalDateTime.now(), new JWTHelper().getCurrentTime());
	}

	private SignedJWT getSignedJWT(final User user, final KeyPair keyPair, final LocalDateTime issueTime, final LocalDateTime validityDate) throws JOSEException {
		ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();

		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES512)
				.type(new JOSEObjectType(JWTHelper.JWT_HEADER_TYP_VALUE))
				.build();
		SignedJWT jwt = new SignedJWT(
				header,
				generateClaims(user, issueTime, validityDate)

		);
		ECDSASigner signer = new ECDSASigner(privateKey.getS());
		jwt.sign(signer);
		return jwt;
	}

	private JWTClaimsSet generateClaims(final User user, final LocalDateTime issueTime, final LocalDateTime validityDate) {
		JWTClaimsSet claimsSet = new JWTClaimsSet();
		claimsSet.setIssuer(user.getPseudo());
		claimsSet.setIssueTime(new DateHelper().toDate(issueTime));
		claimsSet.setExpirationTime(new DateHelper().toDate(validityDate));
		return claimsSet;
	}

	private void assertJWTEquals(final SignedJWT expected, final SignedJWT toTest) throws ParseException {
		assertHeaderEquals(expected.getHeader(), toTest.getHeader());
		assertClaimEquals(expected.getJWTClaimsSet(), toTest.getJWTClaimsSet());
	}

	private void assertHeaderEquals(final JWSHeader expected, final JWSHeader toTest) {
		assertEquals(expected.getAlgorithm(), toTest.getAlgorithm());
		assertEquals(expected.getType(), toTest.getType());
		Map<String, Object> expectedParams = expected.getCustomParams();
		Map<String, Object> toTestParams = toTest.getCustomParams();
		assertEquals(expectedParams.size(), toTestParams.size());
		for (Map.Entry<String, Object> expectedParam : expectedParams.entrySet()) {
			assertEquals(expectedParam.getValue(), toTestParams.get(expectedParam.getKey()));
		}
	}

	private void assertClaimEquals(final ReadOnlyJWTClaimsSet expectedClaimsSet, final ReadOnlyJWTClaimsSet toTestClaimsSet) {
		Map<String, Object> expectedClaims = expectedClaimsSet.getAllClaims();
		Map<String, Object> toTestClaims = toTestClaimsSet.getAllClaims();
		assertEquals(expectedClaims.size(), toTestClaims.size());
		for (Map.Entry<String, Object> expectedClaim : expectedClaims.entrySet()) {
			assertEquals(expectedClaim.getValue(), toTestClaims.get(expectedClaim.getKey()));
		}
	}
}