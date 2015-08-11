package org.datacite.mds.validation.constraints;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DoiPrefixTest extends AbstractContraintsTest {
    @DoiPrefix
    String prefix;

    @Test
    public void test() {
        assertTrue(isValid(null)); 
        assertFalse(isValid(""));
        assertFalse(isValid("abc"));
//        assertFalse(isValid("10"));
//        assertFalse(isValid("10.abc"));
        assertFalse(isValid("10..1234"));
        assertTrue(isValid("10.1234/"));
        assertTrue(isValid("10.1234/test"));
        assertTrue(isValid("10.1234"));
        assertTrue(isValid("10.12345"));
    }

    boolean isValid(String prefix) {
        this.prefix = prefix;
        return super.isValid(this, "prefix");
    }

}
