import httplib2, sys, base64

endpoint = 'https://localhost:8443/mds/igsn'

if (len(sys.argv) < 4):
    raise Exception('Please provide username, password and doi')



h = httplib2.Http()
h.disable_ssl_certificate_validation=True

auth_string = base64.encodestring(sys.argv[1] + ':' + sys.argv[2])
response, content = h.request(endpoint + '/' + sys.argv[3],
                              headers={'Accept':'application/xml',
                                       'Authorization':'Basic ' + auth_string})

if (response.status != 200):
    print str(response.status)
 
print(content.decode('utf-8'))
