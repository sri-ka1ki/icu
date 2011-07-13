/*
 *******************************************************************************
 * Copyright (C) 2009-2011, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.TestUtil;
import com.ibm.icu.dev.test.TestUtil.JavaVendor;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.Normalizer2;
import com.ibm.icu.text.SpoofChecker;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;

public class SpoofCheckerTest extends TestFmwk {

    public static void main(String[] args) throws Exception {
        new SpoofCheckerTest().run(args);
    }

    /*
     * Identifiers for verifying that spoof checking is minimally alive and working.
     */
    char[] goodLatinChars = { (char) 0x75, (char) 0x7a };
    String goodLatin = new String(goodLatinChars); /* "uz", all ASCII */
    /* (not confusable) */
    char[] scMixedChars = { (char) 0x73, (char) 0x0441 };
    String scMixed = new String(scMixedChars); /* "sc", with Cyrillic 'c' */
    /* (mixed script, confusable */

    String scLatin = "sc";   /* "sc", plain ascii. */
    String goodCyrl = "\u0438\u043B";    // "Cyrillic small letter i and el"  Plain lower case Cyrillic letters, no latin confusables 
    String goodGreek = "\u03c0\u03c6";   // "Greek small letter pi and phi"  Plain lower case Greek letters

    // Various 1 l I look-alikes
    String lll_Latin_a = "lI1";   // small letter l, cap I, digit 1, all ASCII
    //  "\uFF29\u217C\u0196"  Full-width I, Small Roman Numeral fifty, Latin Cap Letter IOTA 
    String lll_Latin_b = "\uff29\u217c\u0196";
    String lll_Cyrl = "\u0406\u04C0\u0031";  // "\u0406\u04C01"
    /* The skeleton transform for all of the 'lll' lookalikes is ascii lower case letter l. */
    String lll_Skel = "lll";

    String han_Hiragana = "\u3086\u308A \u77F3\u7530";  // Hiragana, space, Han


    /*
     * Test basic constructor.
     */
    public void TestUSpoof() {
        SpoofChecker sc = new SpoofChecker.Builder().build();
    }

    /*
     * Test build from source rules.
     */
    public void TestOpenFromSourceRules() {
        if (TestUtil.getJavaVendor() == JavaVendor.IBM && TestUtil.getJavaVersion() == 5) {
            // Note: IBM Java 5 has a bug reading a large UTF-8 text contents
            logln("Skip this test case because of the IBM Java 5 bug");
            return;
        }
        SpoofChecker sc = new SpoofChecker.Builder().build();
        String fileName;
        Reader confusables;
        Reader confusablesWholeScript;

        try {
            fileName = "unicode/confusables.txt";
            confusables = TestUtil.getDataReader(fileName, "UTF-8");
            fileName = "unicode/confusablesWholeScript.txt";
            confusablesWholeScript = TestUtil.getDataReader(fileName, "UTF-8");

            SpoofChecker rsc = new SpoofChecker.Builder().setData(confusables, confusablesWholeScript).build();
            if (rsc == null) {
                errln("FAIL: null SpoofChecker");
                return;
            }            
            // Check that newly built-from-rules SpoofChecker is able to function.
            checkSkeleton(rsc, "TestOpenFromSourceRules");
        } catch (java.io.IOException e) {
            errln(e.toString());
        } catch (ParseException e) {
            errln(e.toString());
        }
    }

    /*
     * Set & Get Check Flags
     */
    public void TestGetSetChecks1() {
        SpoofChecker sc = new SpoofChecker.Builder().setChecks(SpoofChecker.ALL_CHECKS).build();
        int t;
        t = sc.getChecks();
        assertEquals("", SpoofChecker.ALL_CHECKS, t);

        sc = new SpoofChecker.Builder().setChecks(0).build();
        t = sc.getChecks();
        assertEquals("", 0, t);

        int checks = SpoofChecker.WHOLE_SCRIPT_CONFUSABLE | SpoofChecker.MIXED_SCRIPT_CONFUSABLE
                | SpoofChecker.ANY_CASE;
        sc = new SpoofChecker.Builder().setChecks(checks).build();
        t = sc.getChecks();
        assertEquals("", checks, t);
    }

    /*
     * get & setAllowedChars
     */
    public void TestGetSetAllowedChars() {
        SpoofChecker sc = new SpoofChecker.Builder().build();
        UnicodeSet us;
        UnicodeSet uset;

        uset = sc.getAllowedChars();
        assertTrue("", uset.isFrozen());
        us = new UnicodeSet((int) 0x41, (int) 0x5A); /* [A-Z] */
        sc = new SpoofChecker.Builder().setAllowedChars(us).build();
        assertEquals("", us, sc.getAllowedChars());
    }

    /*
     * get & set Checks
     */
    public void TestGetSetChecks() {
        SpoofChecker sc = new SpoofChecker.Builder().build();
        int checks;
        int checks2;
        boolean checkResults;

        checks = sc.getChecks();
        assertEquals("", SpoofChecker.ALL_CHECKS, checks);

        checks &= ~(SpoofChecker.SINGLE_SCRIPT | SpoofChecker.MIXED_SCRIPT_CONFUSABLE);
        sc = new SpoofChecker.Builder().setChecks(checks).build();
        checks2 = sc.getChecks();
        assertEquals("", checks, checks2);

        /*
         * The checks that were disabled just above are the same ones that the "scMixed" test fails. So with those tests
         * gone checking that Identifier should now succeed
         */
        checkResults = sc.failsChecks(scMixed);
        assertFalse("", checkResults);
    }

    /*
     * AllowedLocales
     */
    public void TestAllowedLocales() {
        SpoofChecker sc = new SpoofChecker.Builder().build();
        Set<ULocale> allowedLocales = new LinkedHashSet<ULocale>();
        boolean checkResults;

        /* Default allowed locales list should be empty */
        allowedLocales = sc.getAllowedLocales();
        assertTrue("", allowedLocales.isEmpty());

        /* Allow en and ru, which should enable Latin and Cyrillic only to pass */
        ULocale enloc = new ULocale("en");
        ULocale ruloc = new ULocale("ru_RU");
        allowedLocales.add(enloc);
        allowedLocales.add(ruloc);
        sc = new SpoofChecker.Builder().setAllowedLocales(allowedLocales).build();
        allowedLocales = sc.getAllowedLocales();
        assertTrue("", allowedLocales.contains(enloc));
        assertTrue("", allowedLocales.contains(ruloc));

        /*
         * Limit checks to SpoofChecker.CHAR_LIMIT. Some of the test data has whole script confusables also, which we
         * don't want to see in this test.
         */
        sc = new SpoofChecker.Builder().setChecks(SpoofChecker.CHAR_LIMIT).setAllowedLocales(allowedLocales).build();
        
        SpoofChecker.CheckResult result = new SpoofChecker.CheckResult();
        checkResults = sc.failsChecks(goodLatin);
        assertFalse("", checkResults);

        checkResults = sc.failsChecks(goodGreek, result);
        assertEquals("", SpoofChecker.CHAR_LIMIT, result.checks);

        checkResults = sc.failsChecks(goodCyrl);
        assertFalse("", checkResults);

        /* Reset with an empty locale list, which should allow all characters to pass */
        allowedLocales = new LinkedHashSet<ULocale>();
        sc = new SpoofChecker.Builder().setChecks(SpoofChecker.CHAR_LIMIT).setAllowedLocales(allowedLocales).build();

        checkResults = sc.failsChecks(goodGreek);
        assertFalse("", checkResults);
    }

    /*
     * AllowedChars set/get the UnicodeSet of allowed characters.
     */
    public void TestAllowedChars() {
        SpoofChecker sc = new SpoofChecker.Builder().build();
        UnicodeSet set;
        UnicodeSet tmpSet;
        boolean checkResults;

        /* By default, we should see no restriction; the UnicodeSet should allow all characters. */
        set = sc.getAllowedChars();
        tmpSet = new UnicodeSet(0, 0x10ffff);
        assertEquals("", tmpSet, set);

        /* Setting the allowed chars should enable the check. */
        sc = new SpoofChecker.Builder().setChecks(SpoofChecker.ALL_CHECKS & ~SpoofChecker.CHAR_LIMIT).build();

        /* Remove a character that is in our good Latin test identifier from the allowed chars set. */
        tmpSet.remove(goodLatin.charAt(1));
        sc = new SpoofChecker.Builder().setAllowedChars(tmpSet).build();

        /* Latin Identifier should now fail; other non-latin test cases should still be OK */
        SpoofChecker.CheckResult result = new SpoofChecker.CheckResult();
        checkResults = sc.failsChecks(goodLatin, result);
        assertTrue("", checkResults);
        assertEquals("", SpoofChecker.CHAR_LIMIT, result.checks);

        checkResults = sc.failsChecks(goodGreek, result);
        assertTrue("", checkResults);
        assertEquals("", SpoofChecker.WHOLE_SCRIPT_CONFUSABLE, result.checks);
    }

    public void TestCheck() {
        SpoofChecker sc = new SpoofChecker.Builder().build();
        SpoofChecker.CheckResult result = new SpoofChecker.CheckResult();
        boolean checkResults;

        result.position = 666;
        checkResults = sc.failsChecks(goodLatin, result);
        assertFalse("", checkResults);
        assertEquals("", 666, result.position);

        checkResults = sc.failsChecks(goodCyrl, result);
        assertFalse("", checkResults);

        result.position = 666;
        checkResults = sc.failsChecks(scMixed, result);
        assertTrue("", checkResults);
        assertEquals("", SpoofChecker.MIXED_SCRIPT_CONFUSABLE | SpoofChecker.SINGLE_SCRIPT, result.checks);
        assertEquals("", 2, result.position);
        
        result.position = 666;
        checkResults = sc.failsChecks(han_Hiragana, result);
        assertFalse("", checkResults);
        assertEquals("", 666, result.position);
        assertEquals("", 0, result.checks);
    }

    public void TestAreConfusable1() {
        SpoofChecker sc = new SpoofChecker.Builder().build();
        int checkResults;
        checkResults = sc.areConfusable(scLatin, scMixed);
        assertEquals("", SpoofChecker.MIXED_SCRIPT_CONFUSABLE, checkResults);

        checkResults = sc.areConfusable(goodGreek, scLatin);
        assertEquals("", 0, checkResults);

        checkResults = sc.areConfusable(lll_Latin_a, lll_Latin_b);
        assertEquals("", SpoofChecker.SINGLE_SCRIPT_CONFUSABLE, checkResults);
    }

    public void TestGetSkeleton() {
        SpoofChecker sc = new SpoofChecker.Builder().build();
        String dest;
        dest = sc.getSkeleton(SpoofChecker.ANY_CASE, lll_Latin_a);
        assertEquals("", lll_Skel, dest);
    }

    /**
     * IntlTestSpoof is the top level test class for the Unicode Spoof detection tests
     */

    // Test the USpoofDetector API functions that require C++
    // The pure C part of the API, which is most of it, is tested in cintltst
    /**
     * IntlTestSpoof tests for USpoofDetector
     */
    public void TestSpoofAPI() {
        SpoofChecker sc = new SpoofChecker.Builder().build();
        String s = "xyz";  // Many latin ranges are whole-script confusable with other scripts.
                           // If this test starts failing, consult confusablesWholeScript.txt
        SpoofChecker.CheckResult result = new SpoofChecker.CheckResult();
        result.position = 666;
        boolean checkResults = sc.failsChecks(s, result);
        assertFalse("", checkResults);
        assertEquals("", 666, result.position);   // not changed

        sc = new SpoofChecker.Builder().build();
        String s1 = "cxs";
        String s2 = Utility.unescape("\\u0441\\u0445\\u0455"); // Cyrillic "cxs"
        int checkResult = sc.areConfusable(s1, s2);
        assertEquals("", SpoofChecker.MIXED_SCRIPT_CONFUSABLE | SpoofChecker.WHOLE_SCRIPT_CONFUSABLE, checkResult);

        sc = new SpoofChecker.Builder().build();
        s = "I1l0O";
        String dest = sc.getSkeleton(SpoofChecker.ANY_CASE, s);
        assertEquals("", dest, "lllOO");
    }

    public void TestSkeleton() {
        SpoofChecker sc = new SpoofChecker.Builder().build();
        checkSkeleton(sc, "TestSkeleton");
    }
    
    // testSkeleton. Spot check a number of confusable skeleton substitutions from the
    // Unicode data file confusables.txt
    // Test cases chosen for substitutions of various lengths, and
    // membership in different mapping tables.
    public void checkSkeleton(SpoofChecker sc, String testName) {
        int ML = 0;
        int SL = SpoofChecker.SINGLE_SCRIPT_CONFUSABLE;
        int MA = SpoofChecker.ANY_CASE;
        int SA = SpoofChecker.SINGLE_SCRIPT_CONFUSABLE | SpoofChecker.ANY_CASE;

        // A long "identifier" that will overflow implementation stack buffers, forcing heap allocations.
        //    (in the C implementation)
        checkSkeleton(
                sc,
                SL,
                " A 1ong \\u02b9identifier' that will overflow implementation stack buffers, forcing heap allocations."
                        + " A 1ong 'identifier' that will overflow implementation stack buffers, forcing heap allocations."
                        + " A 1ong 'identifier' that will overflow implementation stack buffers, forcing heap allocations."
                        + " A 1ong 'identifier' that will overflow implementation stack buffers, forcing heap allocations.",
                " A long 'identifier' that vvill overflovv irnplernentation stack buffers, forcing heap allocations."
                        + " A long 'identifier' that vvill overflovv irnplernentation stack buffers, forcing heap allocations."
                        + " A long 'identifier' that vvill overflovv irnplernentation stack buffers, forcing heap allocations."
                        + " A long 'identifier' that vvill overflovv irnplernentation stack buffers, forcing heap allocations.",
                testName);

        checkSkeleton(sc, SL, "nochange", "nochange", testName);
        checkSkeleton(sc, MA, "love", "love", testName);
        checkSkeleton(sc, MA, "1ove", "love", testName);   // Digit 1 to letter l
        checkSkeleton(sc, ML, "OOPS", "OOPS", testName);
        checkSkeleton(sc, ML, "00PS", "00PS", testName);   // Digit 0 unchanged in lower case mode.
        checkSkeleton(sc, MA, "OOPS", "OOPS", testName);
        checkSkeleton(sc, MA, "00PS", "OOPS", testName);   // Digit 0 to letter O in any case mode only
        checkSkeleton(sc, SL, "\\u059c", "\\u0301", testName);
        checkSkeleton(sc, SL, "\\u2A74", "\\u003A\\u003A\\u003D", testName);
        checkSkeleton(sc, SL, "\\u247E", "\\u0028\\u006c\\u006c\\u0029", testName);  // "(ll)"
        checkSkeleton(sc, SL, "\\uFDFB", "\\u062C\\u0644\\u0020\\u062C\\u0644\\u0627\\u0644\\u0647", testName);

        // This mapping exists in the ML and MA tables, does not exist in SL, SA
        // 0C83 ; 0983 ; ML #  KANNADA SIGN VISARGA to 
        checkSkeleton(sc, SL, "\\u0C83", "\\u0C83", testName);
        checkSkeleton(sc, SA, "\\u0C83", "\\u0C83", testName);
        checkSkeleton(sc, ML, "\\u0C83", "\\u0983", testName);
        checkSkeleton(sc, MA, "\\u0C83", "\\u0983", testName);

        // 0391 ; 0041 ; MA # GREEK CAPITAL LETTER ALPHA to LATIN CAPITAL LETTER A
        // This mapping exists only in the MA table.
        checkSkeleton(sc, MA, "\\u0391", "A", testName);
        checkSkeleton(sc, SA, "\\u0391", "\\u0391", testName);
        checkSkeleton(sc, ML, "\\u0391", "\\u0391", testName);
        checkSkeleton(sc, SL, "\\u0391", "\\u0391", testName);

        // 13CF ; 0062 ; MA # CHEROKEE LETTER SI to LATIN SMALL LETTER B
        // This mapping exists in the ML and MA tables
        checkSkeleton(sc, ML, "\\u13CF", "b", testName);
        checkSkeleton(sc, MA, "\\u13CF", "b", testName);
        checkSkeleton(sc, SL, "\\u13CF", "\\u13CF", testName);
        checkSkeleton(sc, SA, "\\u13CF", "\\u13CF", testName);

        // 0022 ; 0027 0027 ; 
        // all tables
        checkSkeleton(sc, SL, "\"", "\\u0027\\u0027", testName);
        checkSkeleton(sc, SA, "\"", "\\u0027\\u0027", testName);
        checkSkeleton(sc, ML, "\"", "\\u0027\\u0027", testName);
        checkSkeleton(sc, MA, "\"", "\\u0027\\u0027", testName);
    }

    // Internal function to run a single skeleton test case.
    //
    // Run a single confusable skeleton transformation test case.
    //
    void checkSkeleton(SpoofChecker sc, int type, String input, String expected, String testName) {
        String uInput = Utility.unescape(input);
        String uExpected = Utility.unescape(expected);
        String actual;
        actual = sc.getSkeleton(type, uInput);
        assertEquals(testName + "Expected (escaped): " + expected, uExpected, actual);
    }

    public void TestAreConfusable() {
        SpoofChecker sc = new SpoofChecker.Builder().build();
        String s1 = "A long string that will overflow stack buffers.  A long string that will overflow stack buffers. "
                + "A long string that will overflow stack buffers.  A long string that will overflow stack buffers. ";
        String s2 = "A long string that wi11 overflow stack buffers.  A long string that will overflow stack buffers. "
                + "A long string that wi11 overflow stack buffers.  A long string that will overflow stack buffers. ";
        assertEquals("", SpoofChecker.SINGLE_SCRIPT_CONFUSABLE, sc.areConfusable(s1, s2));
    }

    public void TestInvisible() {
        SpoofChecker sc = new SpoofChecker.Builder().build();
        String s = Utility.unescape("abcd\\u0301ef");
        SpoofChecker.CheckResult result = new SpoofChecker.CheckResult();
        result.position = -42;
        assertFalse("", sc.failsChecks(s, result));
        assertEquals("", 0, result.checks);
        assertEquals("", result.position, -42); // unchanged

        String s2 = Utility.unescape("abcd\\u0301\\u0302\\u0301ef");
        assertTrue("", sc.failsChecks(s2, result));
        assertEquals("", SpoofChecker.INVISIBLE, result.checks);
        assertEquals("", 7, result.position);

        // Two acute accents, one from the composed a with acute accent, \u00e1,
        // and one separate.
        result.position = -42;
        String s3 = Utility.unescape("abcd\\u00e1\\u0301xyz");
        assertTrue("", sc.failsChecks(s3, result));
        assertEquals("", SpoofChecker.INVISIBLE, result.checks);
        assertEquals("", 7, result.position);
    }

    private String parseHex(String in) {
        StringBuilder sb = new StringBuilder();
        for (String oneCharAsHexString : in.split("\\s+")) {
            if (oneCharAsHexString.length() > 0) {
                sb.appendCodePoint(Integer.parseInt(oneCharAsHexString, 16));
            }
        }
        return sb.toString();
    }

    private String escapeString(String in) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < in.length(); i++) {
            int c = in.codePointAt(i);
            if (c <= 0x7f) {
                out.append((char) c);
            } else if (c <= 0xffff) {
                out.append(String.format("\\u%04x", c));
            } else {
                out.append(String.format("\\U%06x", c));
                i++;
            }
        }
        return out.toString();
    }

    // Verify that each item from the Unicode confusables.txt file
    // transforms into the expected skeleton.
    public void testConfData() {
        if (TestUtil.getJavaVendor() == JavaVendor.IBM && TestUtil.getJavaVersion() == 5) {
            // Note: IBM Java 5 has a bug reading a large UTF-8 text contents
            logln("Skip this test case because of the IBM Java 5 bug");
            return;
        }
        try {
            // Read in the confusables.txt file. (Distributed by Unicode.org)
            String fileName = "unicode/confusables.txt";
            BufferedReader confusablesRdr = TestUtil.getDataReader(fileName, "UTF-8");

            // Create a default spoof checker to use in this test.
            SpoofChecker sc = new SpoofChecker.Builder().build();

            // Parse lines from the confusables.txt file. Example Line:
            // FF44 ; 0064 ; SL # ( d -> d ) FULLWIDTH ....
            // Lines have three fields. The hex fields can contain more than one character,
            // and each character may be more than 4 digits (for supplemntals)
            // This regular expression matches lines and splits the fields into capture groups.
            // Capture group 1: map from chars
            // 2: map to chars
            // 3: table type, SL, ML, SA or MA
            // 4: Comment Lines Only
            // 5: Error Lines Only
            Matcher parseLine = Pattern.compile(
                    "\\ufeff?" + "(?:([0-9A-F\\s]+);([0-9A-F\\s]+);\\s*(SL|ML|SA|MA)\\s*(?:#.*?)?$)"
                            + "|\\ufeff?(\\s*(?:#.*)?)"). // Comment line
                    matcher("");
            Normalizer2 normalizer = Normalizer2.getInstance(null, "nfc", Normalizer2.Mode.DECOMPOSE);
            int lineNum = 0;
            String inputLine;
            while ((inputLine = confusablesRdr.readLine()) != null) {
                lineNum++;
                parseLine.reset(inputLine);
                if (!parseLine.matches()) {
                    errln("Syntax error in confusable data file at line " + lineNum);
                    errln(inputLine);
                    break;
                }
                if (parseLine.group(4) != null) {
                    continue; // comment line
                }
                String from = parseHex(parseLine.group(1));

                if (!normalizer.isNormalized(from)) {
                    // The source character was not NFD.
                    // Skip this case; the first step in obtaining a skeleton is to NFD the input,
                    // so the mapping in this line of confusables.txt will never be applied.
                    continue;
                }

                String rawExpected = parseHex(parseLine.group(2));
                String expected = normalizer.normalize(rawExpected);

                int skeletonType = 0;
                String tableType = parseLine.group(3);
                if (tableType.equals("SL")) {
                    skeletonType = SpoofChecker.SINGLE_SCRIPT_CONFUSABLE;
                } else if (tableType.indexOf("SA") >= 0) {
                    skeletonType = SpoofChecker.SINGLE_SCRIPT_CONFUSABLE | SpoofChecker.ANY_CASE;
                } else if (tableType.indexOf("ML") >= 0) {
                    skeletonType = 0;
                } else if (tableType.indexOf("MA") >= 0) {
                    skeletonType = SpoofChecker.ANY_CASE;
                }

                String actual;
                actual = sc.getSkeleton(skeletonType, from);

                if (!actual.equals(expected)) {
                    errln("confusables.txt: " + lineNum + ": " + parseLine.group(0));
                    errln("Actual: " + escapeString(actual));
                }
            }
            confusablesRdr.close();
        } catch (IOException e) {
            errln(e.toString());
        }
    }
}
