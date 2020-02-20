package org.datacite.mds.service.impl;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.codec.Utf8;
import org.springframework.stereotype.Service;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.datacite.mds.service.PasswordEncoder;

@Service
public class PasswordEncoderImpl implements PasswordEncoder {

    Logger log4j = Logger.getLogger(PasswordEncoderImpl.class);

    @Value("${salt.password}")
    String salt;
    String algorithm = "SHA-256";

    public PasswordEncoderImpl() {
    }

    // @Override
    public String encodePassword(String rawPass, Object salt) {
        // use system-wide salt
        log4j.debug("encodePassword (salt=" + this.salt + ")");

     //   log4j.debug("passwd:" + rawPass);

        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            String saltedPass; 

            if ((this.salt == null) || "".equals(this.salt)) {
                saltedPass = rawPass;
            } else {
                saltedPass = rawPass + "{" + this.salt + "}";
            }

            byte[] digested = md.digest(Utf8.encode(saltedPass));
            return new String(Hex.encode(digested));
        } catch (NoSuchAlgorithmException e) {
        }

        return null;

    }

    // @Override
    public boolean isPasswordValid(String encPass, String rawPass, Object salt) {
        // use system-wide salt
        String validatepass = encodePassword(rawPass, salt);
        return encPass.equals(validatepass);
    }

    @Override
    public String encode(CharSequence rawPassword) {
        return encodePassword(rawPassword.toString(), this.salt);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return isPasswordValid(encodedPassword.toString(), rawPassword.toString(), this.salt);
    }

}
