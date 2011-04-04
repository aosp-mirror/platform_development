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
Defines Django forms for inserting/updating/viewing contact data
to/from SampleSyncAdapter datastore.
"""

import cgi
import datetime
import os

from google.appengine.ext import db
from google.appengine.ext import webapp
from google.appengine.ext.webapp import template
from google.appengine.ext.db import djangoforms
from model import datastore
from google.appengine.api import images

import wsgiref.handlers

class BaseRequestHandler(webapp.RequestHandler):
    """
    Base class for our page-based request handlers that contains
    some helper functions we use in most pages.
    """

    """
    Return a form (potentially partially filled-in) to
    the user.
    """
    def send_form(self, title, action, contactId, handle, content_obj):
        if (contactId >= 0):
            idInfo = '<input type="hidden" name="_id" value="%s">'
        else:
            idInfo = ''

        template_values = {
                'title': title,
                'header': title,
                'action': action,
                'contactId': contactId,
                'handle': handle,
                'has_contactId': (contactId >= 0),
                'has_handle': (handle != None),
                'form_data_rows': str(content_obj)
                }

        path = os.path.join(os.path.dirname(__file__), 'templates', 'simple_form.html')
        self.response.out.write(template.render(path, template_values))

class ContactForm(djangoforms.ModelForm):
    """Represents django form for entering contact info."""

    class Meta:
        model = datastore.Contact


class ContactInsertPage(BaseRequestHandler):
    """
    Processes requests to add a new contact. GET presents an empty
    contact form for the user to fill in.  POST saves the new contact
    with the POSTed information.
    """

    def get(self):
        self.send_form('Add Contact', '/add_contact', -1, None, ContactForm())

    def post(self):
        data = ContactForm(data=self.request.POST)
        if data.is_valid():
            # Save the data, and redirect to the view page
            entity = data.save(commit=False)
            entity.put()
            self.redirect('/')
        else:
            # Reprint the form
            self.send_form('Add Contact', '/add_contact', -1, None, data)


class ContactEditPage(BaseRequestHandler):
    """
    Process requests to edit a contact's information.  GET presents a form
    with the current contact information filled in. POST saves new information
    into the contact record.
    """

    def get(self):
        id = int(self.request.get('id'))
        contact = datastore.Contact.get(db.Key.from_path('Contact', id))
        self.send_form('Edit Contact', '/edit_contact', id, contact.handle, 
                       ContactForm(instance=contact))

    def post(self):
        id = int(self.request.get('id'))
        contact = datastore.Contact.get(db.Key.from_path('Contact', id))
        data = ContactForm(data=self.request.POST, instance=contact)
        if data.is_valid():
            # Save the data, and redirect to the view page
            entity = data.save(commit=False)
            entity.updated = datetime.datetime.utcnow()
            entity.put()
            self.redirect('/')
        else:
            # Reprint the form
            self.send_form('Edit Contact', '/edit_contact', id, contact.handle, data)

class ContactDeletePage(BaseRequestHandler):
    """Processes delete contact request."""

    def get(self):
        id = int(self.request.get('id'))
        contact = datastore.Contact.get(db.Key.from_path('Contact', id))
        contact.deleted = True
        contact.updated = datetime.datetime.utcnow()
        contact.put()

        self.redirect('/')

class AvatarEditPage(webapp.RequestHandler):
    """
    Processes requests to edit contact's avatar. GET is used to fetch
    a page that displays the contact's current avatar and allows the user 
    to specify a file containing a new avatar image.  POST is used to
    submit the form which will change the contact's avatar.
    """

    def get(self):
        id = int(self.request.get('id'))
        contact = datastore.Contact.get(db.Key.from_path('Contact', id))
        template_values = {
                'avatar': contact.avatar,
                'contactId': id
                }
        
        path = os.path.join(os.path.dirname(__file__), 'templates', 'edit_avatar.html')
        self.response.out.write(template.render(path, template_values))

    def post(self):
        id = int(self.request.get('id'))
        contact = datastore.Contact.get(db.Key.from_path('Contact', id))
        #avatar = images.resize(self.request.get("avatar"), 128, 128)
        avatar = self.request.get("avatar")
        contact.avatar = db.Blob(avatar)
        contact.updated = datetime.datetime.utcnow()
        contact.put()
        self.redirect('/')

class AvatarViewPage(BaseRequestHandler):
    """
    Processes request to view contact's avatar. This is different from
    the GET AvatarEditPage request in that this doesn't return a page -
    it just returns the raw image itself.
    """

    def get(self):
        id = int(self.request.get('id'))
        contact = datastore.Contact.get(db.Key.from_path('Contact', id))
        if (contact.avatar):
            self.response.headers['Content-Type'] = "image/png"
            self.response.out.write(contact.avatar)
        else:
            self.redirect(self.request.host_url + '/static/img/default_avatar.gif')

class ContactsListPage(webapp.RequestHandler):
    """
    Display a page that lists all the contacts associated with
    the specifies user account.
    """

    def get(self):
        contacts = datastore.Contact.all()
        template_values = {
                'contacts': contacts,
                'username': 'user'
                }

        path = os.path.join(os.path.dirname(__file__), 'templates', 'contacts.html')
        self.response.out.write(template.render(path, template_values))


def main():
    application = webapp.WSGIApplication(
        [('/', ContactsListPage),
         ('/add_contact', ContactInsertPage),
         ('/edit_contact', ContactEditPage),
         ('/delete_contact', ContactDeletePage),
         ('/avatar', AvatarViewPage),
         ('/edit_avatar', AvatarEditPage)
        ],
        debug=True)
    wsgiref.handlers.CGIHandler().run(application)

if __name__ == '__main__':
  main()