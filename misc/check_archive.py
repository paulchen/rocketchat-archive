#!/usr/bin/python3

import requests, sys, os

for key in ('ARCHIVE_BASE_URL', 'ARCHIVE_USERNAME', 'ARCHIVE_PASSWORD'):
    if key not in os.environ:
        print('Environment variable %s not set' % key)
        sys.exit(3)

base_url = os.environ['ARCHIVE_BASE_URL']
username = os.environ['ARCHIVE_USERNAME']
password = os.environ['ARCHIVE_PASSWORD']

session = requests.Session()
session.auth = (username, password)

# frontend revision
r = session.get(base_url + '/git-version.json')
if not r:
    print('UNKNOWN: Status code %s received from frontend' % r.status_code)
    sys.exit(3)

j = r.json()
frontend_revision = j['shortSHA']

# backend revision
r = session.get(base_url + '/services/version')
if not r:
    print('UNKNOWN: Status code %s received from backend' % r.status_code)
    sys.exit(3)

j = r.json()
backend_revision = j['version']

if frontend_revision != backend_revision:
    print('CRITICAL: Frontend revision (%s) and backend revision (%s) do not match' % (frontend_revision, backend_revision))
    sys.exit(2)

# fetch channels list
r = session.get(base_url + '/services/channels')
if not r:
    print('UNKNOWN: Status code %s received from backend' % r.status_code)
    sys.exit(3)

j = r.json()
channels = len(j['channels'])

if channels == 0:
    print('CRITICAL: No channels found')
    sys.exit(2)

print('OK: Git revision %s deployed on both frontend and backend, %s channels found' % (frontend_revision, channels))

