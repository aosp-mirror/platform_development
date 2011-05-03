#!/usr/bin/python2.5

# Copyright (C) 2010 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.

"""
Handlers for Sample SyncAdapter services.

Contains several RequestHandler subclasses used to handle post operations.
This script is designed to be run directly as a WSGI application.

"""

import cgi
import logging
import time as _time
from datetime import datetime
from django.utils import simplejson
from google.appengine.api import users
from google.appengine.ext import db
from google.appengine.ext import webapp
from model import datastore
import wsgiref.handlers


class BaseWebServiceHandler(webapp.RequestHandler):
    """
    Base class for our web services. We put some common helper
    functions here.
    """

    """
    Since we're only simulating a single user account, declare our
    hard-coded credentials here, so that they're easy to see/find.
    We actually accept any and all usernames that start with this
    hard-coded values. So if ACCT_USER_NAME is 'user', then we'll
    accept 'user', 'user75', 'userbuddy', etc, all as legal account
    usernames.
    """
    ACCT_USER_NAME  = 'user'
    ACCT_PASSWORD   = 'test'
    ACCT_AUTH_TOKEN = 'xyzzy'

    DATE_TIME_FORMAT = '%Y/%m/%d %H:%M'

    """
    Process a request to authenticate a client.  We assume that the username
    and password will be included in the request. If successful, we'll return
    an authtoken as the only content.  If auth fails, we'll send an "invalid
    credentials" error.
    We return a boolean indicating whether we were successful (true) or not (false).
    In the event that this call fails, we will setup the response, so callers just
    need to RETURN in the error case.
    """
    def authenticate(self):
        self.username = self.request.get('username')
        self.password = self.request.get('password')

        logging.info('Authenticatng username: ' + self.username)

        if ((self.username != None) and
                (self.username.startswith(BaseWebServiceHandler.ACCT_USER_NAME)) and
                (self.password == BaseWebServiceHandler.ACCT_PASSWORD)):
            # Authentication was successful - return our hard-coded
            # auth-token as the only response.
            self.response.set_status(200, 'OK')
            self.response.out.write(BaseWebServiceHandler.ACCT_AUTH_TOKEN)
            return True
        else:
            # Authentication failed. Return the standard HTTP auth failure
            # response to let the client know.
            self.response.set_status(401, 'Invalid Credentials')
            return False

    """
    Validate the credentials of the client for a web service request.
    The request should include username/password parameters that correspond
    to our hard-coded single account values.
    We return a boolean indicating whether we were successful (true) or not (false).
    In the event that this call fails, we will setup the response, so callers just
    need to RETURN in the error case.
    """
    def validate(self):
        self.username = self.request.get('username')
        self.authtoken = self.request.get('authtoken')

        logging.info('Validating username: ' + self.username)

        if ((self.username != None) and
                (self.username.startswith(BaseWebServiceHandler.ACCT_USER_NAME)) and
                (self.authtoken == BaseWebServiceHandler.ACCT_AUTH_TOKEN)):
            return True
        else:
            self.response.set_status(401, 'Invalid Credentials')
            return False


class Authenticate(BaseWebServiceHandler):
    """
    Handles requests for login and authentication.

    UpdateHandler only accepts post events. It expects each
    request to include username and password fields. It returns authtoken
    after successful authentication and "invalid credentials" error otherwise.
    """

    def post(self):
        self.authenticate()

    def get(self):
        """Used for debugging in a browser..."""
        self.post()


class SyncContacts(BaseWebServiceHandler):
    """Handles requests for fetching user's contacts.

    UpdateHandler only accepts post events. It expects each
    request to include username and authtoken. If the authtoken is valid
    it returns user's contact info in JSON format.
    """

    def get(self):
        """Used for debugging in a browser..."""
        self.post()

    def post(self):
        logging.info('*** Starting contact sync ***')
        if (not self.validate()):
            return

        updated_contacts = []

        # Process any client-side changes sent up in the request.
        # Any new contacts that were added are included in the
        # updated_contacts list, so that we return them to the
        # client. That way, the client can see the serverId of
        # the newly added contact.
        client_buffer = self.request.get('contacts')
        if ((client_buffer != None) and (client_buffer != '')):
            self.process_client_changes(client_buffer, updated_contacts)

        # Add any contacts that have been updated on the server-side
        # since the last sync by this client.
        client_state = self.request.get('syncstate')
        self.get_updated_contacts(client_state, updated_contacts)

        logging.info('Returning ' + str(len(updated_contacts)) + ' contact records')

        # Return the list of updated contacts to the client
        self.response.set_status(200)
        self.response.out.write(toJSON(updated_contacts))

    def get_updated_contacts(self, client_state, updated_contacts):
        logging.info('* Processing server changes')
        timestamp = None

        base_url = self.request.host_url

        # The client sends the last high-water-mark that they successfully
        # sync'd to in the syncstate parameter.  It's opaque to them, but
        # its actually a seconds-in-unix-epoch timestamp that we use
        # as a baseline.
        if client_state:
            logging.info('Client sync state: ' + client_state)
            timestamp = datetime.utcfromtimestamp(float(client_state))

        # Keep track of the update/delete counts, so we can log it
        # below.  Makes debugging easier...
        update_count = 0
        delete_count = 0

        contacts = datastore.Contact.all()
        if contacts:
            # Find the high-water mark for the most recently updated friend.
            # We'll return this as the syncstate (x) value for all the friends
            # we return from this function.
            high_water_date = datetime.min
            for contact in contacts:
                if (contact.updated > high_water_date):
                    high_water_date = contact.updated
            high_water_mark = str(long(_time.mktime(high_water_date.utctimetuple())) + 1)
            logging.info('New sync state: ' + high_water_mark)

            # Now build the updated_contacts containing all the friends that have been
            # changed since the last sync
            for contact in contacts:
                # If our list of contacts we're returning already contains this
                # contact (for example, it's a contact just uploaded from the client)
                # then don't bother processing it any further...
                if (self.list_contains_contact(updated_contacts, contact)):
                    continue

                handle = contact.handle

                if timestamp is None or contact.updated > timestamp:
                    if contact.deleted == True:
                        delete_count = delete_count + 1
                        DeletedContactData(updated_contacts, handle, high_water_mark)
                    else:
                        update_count = update_count + 1
                        UpdatedContactData(updated_contacts, handle, None, base_url, high_water_mark)

        logging.info('Server-side updates: ' + str(update_count))
        logging.info('Server-side deletes: ' + str(delete_count))

    def process_client_changes(self, contacts_buffer, updated_contacts):
        logging.info('* Processing client changes: ' + self.username)

        base_url = self.request.host_url

        # Build an array of generic objects containing contact data,
        # using the Django built-in JSON parser
        logging.info('Uploaded contacts buffer: ' + contacts_buffer)
        json_list = simplejson.loads(contacts_buffer)
        logging.info('Client-side updates: ' + str(len(json_list)))

        # Keep track of the number of new contacts the client sent to us,
        # so that we can log it below.
        new_contact_count = 0

        for jcontact in json_list:
            new_contact = False
            id = self.safe_attr(jcontact, 'i')
            if (id != None):
                logging.info('Updating contact: ' + str(id))
                contact = datastore.Contact.get(db.Key.from_path('Contact', id))
            else:
                logging.info('Creating new contact record')
                new_contact = True
                contact = datastore.Contact(handle='temp')

            # If the 'change' for this contact is that they were deleted
            # on the client-side, all we want to do is set the deleted
            # flag here, and we're done.
            if (self.safe_attr(jcontact, 'd') == True):
                contact.deleted = True
                contact.put()
                logging.info('Deleted contact: ' + contact.handle)
                continue

            contact.firstname = self.safe_attr(jcontact, 'f')
            contact.lastname = self.safe_attr(jcontact, 'l')
            contact.phone_home = self.safe_attr(jcontact, 'h')
            contact.phone_office = self.safe_attr(jcontact, 'o')
            contact.phone_mobile = self.safe_attr(jcontact, 'm')
            contact.email = self.safe_attr(jcontact, 'e')
            contact.deleted = (self.safe_attr(jcontact, 'd') == 'true')
            if (new_contact):
                # New record - add them to db...
                new_contact_count = new_contact_count + 1
                contact.handle = contact.firstname + '_' + contact.lastname
                logging.info('Created new contact handle: ' + contact.handle)
            contact.put()
            logging.info('Saved contact: ' + contact.handle)

            # We don't save off the client_id value (thus we add it after
            # the "put"), but we want it to be in the JSON object we
            # serialize out, so that the client can match this contact
            # up with the client version.
            client_id = self.safe_attr(jcontact, 'c')

            # Create a high-water-mark for sync-state from the 'updated' time
            # for this contact, so we return the correct value to the client.
            high_water = str(long(_time.mktime(contact.updated.utctimetuple())) + 1)

            # Add new contacts to our updated_contacts, so that we return them
            # to the client (so the client gets the serverId for the
            # added contact)
            if (new_contact):
                UpdatedContactData(updated_contacts, contact.handle, client_id, base_url,
                        high_water)

        logging.info('Client-side adds: ' + str(new_contact_count))

    def list_contains_contact(self, contact_list, contact):
        if (contact is None):
            return False
        contact_id = str(contact.key().id())
        for next in contact_list:
            if ((next != None) and (next['i'] == contact_id)):
                return True
        return False

    def safe_attr(self, obj, attr_name):
        if attr_name in obj:
            return obj[attr_name]
        return None

class ResetDatabase(BaseWebServiceHandler):
    """
    Handles cron request to reset the contact database.

    We have a weekly cron task that resets the database back to a
    few contacts, so that it doesn't grow to an absurd size.
    """

    def get(self):
        # Delete all the existing contacts from the database
        contacts = datastore.Contact.all()
        for contact in contacts:
            contact.delete()

        # Now create three sample contacts
        contact1 = datastore.Contact(handle = 'juliet',
                firstname = 'Juliet',
                lastname = 'Capulet',
                phone_mobile = '(650) 555-1000',
                phone_home = '(650) 555-1001',
                status = 'Wherefore art thou Romeo?')
        contact1.put()

        contact2 = datastore.Contact(handle = 'romeo',
                firstname = 'Romeo',
                lastname = 'Montague',
                phone_mobile = '(650) 555-2000',
                phone_home = '(650) 555-2001',
                status = 'I dream\'d a dream to-night')
        contact2.put()

        contact3 = datastore.Contact(handle = 'tybalt',
                firstname = 'Tybalt',
                lastname = 'Capulet',
                phone_mobile = '(650) 555-3000',
                phone_home = '(650) 555-3001',
                status = 'Have at thee, coward')
        contact3.put()




def toJSON(object):
    """Dumps the data represented by the object to JSON for wire transfer."""
    return simplejson.dumps(object)

class UpdatedContactData(object):
    """Holds data for user's contacts.

    This class knows how to serialize itself to JSON.
    """
    __FIELD_MAP = {
        'handle': 'u',
        'firstname': 'f',
        'lastname': 'l',
        'status': 's',
        'phone_home': 'h',
        'phone_office': 'o',
        'phone_mobile': 'm',
        'email': 'e',
        'client_id': 'c'
    }

    def __init__(self, contact_list, username, client_id, host_url, high_water_mark):
        obj = datastore.Contact.get_contact_info(username)
        contact = {}
        for obj_name, json_name in self.__FIELD_MAP.items():
            if hasattr(obj, obj_name):
              v = getattr(obj, obj_name)
              if (v != None):
                  contact[json_name] = str(v)
              else:
                  contact[json_name] = None
        contact['i'] = str(obj.key().id())
        contact['a'] = host_url + "/avatar?id=" + str(obj.key().id())
        contact['x'] = high_water_mark
        if (client_id != None):
            contact['c'] = str(client_id)
        contact_list.append(contact)

class DeletedContactData(object):
    def __init__(self, contact_list, username, high_water_mark):
        obj = datastore.Contact.get_contact_info(username)
        contact = {}
        contact['d'] = 'true'
        contact['i'] = str(obj.key().id())
        contact['x'] = high_water_mark
        contact_list.append(contact)

def main():
    application = webapp.WSGIApplication(
            [('/auth', Authenticate),
             ('/sync', SyncContacts),
             ('/reset_database', ResetDatabase),
            ],
            debug=True)
    wsgiref.handlers.CGIHandler().run(application)

if __name__ == "__main__":
    main()
