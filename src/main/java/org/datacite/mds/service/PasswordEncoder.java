package org.datacite.mds.service;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.MessageDigestPasswordEncoder;
import org.springframework.stereotype.Service;
import java.nio.charset.Charset;

@Service
public interface PasswordEncoder extends org.springframework.security.crypto.password.PasswordEncoder {

    public String encodePassword(String rawPass, Object salt);

    public boolean isPasswordValid(String encPass, String rawPass, Object salt);

}
