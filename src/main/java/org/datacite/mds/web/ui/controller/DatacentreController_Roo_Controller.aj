// WARNING: DO NOT EDIT THIS FILE. THIS FILE IS MANAGED BY SPRING ROO.
// You may push code into the target .java compilation unit if you wish to edit any member(s).

package org.datacite.mds.web.ui.controller;

import java.io.UnsupportedEncodingException;
import java.lang.String;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.WebUtils;

privileged aspect DatacentreController_Roo_Controller {
    
    String DatacentreController.encodeUrlPathSegment(String pathSegment, HttpServletRequest httpServletRequest) {
        String enc = httpServletRequest.getCharacterEncoding();
        if (enc == null) {
            enc = WebUtils.DEFAULT_CHARACTER_ENCODING;
        }
 //       try {
            pathSegment = UriUtils.encodePathSegment(pathSegment, enc);
 //       }
 //       catch (UnsupportedEncodingException uee) {}
        return pathSegment;
    }
    
}
