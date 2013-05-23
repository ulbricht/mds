package org.datacite.mds.service;

import org.datacite.mds.service.ProxyException;
import org.datacite.mds.web.api.NotFoundException;

public interface ProxyService{
    
    public String doiResolve(String doi) throws NotFoundException;

    public void doiUpdate(String doi, String newUrl) throws ProxyException, IllegalArgumentException;
    
    public void metaUpdate(byte[] xml) throws ProxyException;

    public void metaDelete(String doi) throws ProxyException, NotFoundException;

	 public boolean isProxyMode();

    
}
