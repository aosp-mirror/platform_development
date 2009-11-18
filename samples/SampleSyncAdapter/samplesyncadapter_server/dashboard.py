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

"""Defines Django forms for inserting/updating/viewing data
   to/from SampleSyncAdapter datastore."""

import cgi
import datetime
import os

from google.appengine.ext import db
from google.appengine.ext import webapp
from google.appengine.ext.webapp import template
from google.appengine.ext.db import djangoforms
from model import datastore

import wsgiref.handlers


class UserForm(djangoforms.ModelForm):
  """Represents django form for entering user info."""

  class Meta:
    model = datastore.User


class UserInsertPage(webapp.RequestHandler):
  """Inserts new users. GET presents a blank form. POST processes it."""

  def get(self):
    self.response.out.write('<html><body>'
                            '<form method="POST" '
                            'action="/add_user">'
                            '<table>')
    # This generates our shopping list form and writes it in the response
    self.response.out.write(UserForm())
    self.response.out.write('</table>'
                            '<input type="submit">'
                            '</form></body></html>')

  def post(self):
    data = UserForm(data=self.request.POST)
    if data.is_valid():
      # Save the data, and redirect to the view page
      entity = data.save(commit=False)
      entity.put()
      self.redirect('/users')
    else:
      # Reprint the form
      self.response.out.write('<html><body>'
                              '<form method="POST" '
                              'action="/">'
                              '<table>')
      self.response.out.write(data)
      self.response.out.write('</table>'
                              '<input type="submit">'
                              '</form></body></html>')


class UserEditPage(webapp.RequestHandler):
  """Edits users. GET presents a form prefilled with user info
     from datastore. POST processes it."""

  def get(self):
    id = int(self.request.get('user'))
    user = datastore.User.get(db.Key.from_path('User', id))
    self.response.out.write('<html><body>'
                            '<form method="POST" '
                            'action="/edit_user">'
                            '<table>')
    # This generates our shopping list form and writes it in the response
    self.response.out.write(UserForm(instance=user))
    self.response.out.write('</table>'
                            '<input type="hidden" name="_id" value="%s">'
                            '<input type="submit">'
                            '</form></body></html>' % id)

  def post(self):
    id = int(self.request.get('_id'))
    user = datastore.User.get(db.Key.from_path('User', id))
    data = UserForm(data=self.request.POST, instance=user)
    if data.is_valid():
      # Save the data, and redirect to the view page
      entity = data.save(commit=False)
      entity.updated = datetime.datetime.utcnow()
      entity.put()
      self.redirect('/users')
    else:
      # Reprint the form
      self.response.out.write('<html><body>'
                              '<form method="POST" '
                              'action="/edit_user">'
                              '<table>')
      self.response.out.write(data)
      self.response.out.write('</table>'
                              '<input type="hidden" name="_id" value="%s">'
                              '<input type="submit">'
                              '</form></body></html>' % id)


class UsersListPage(webapp.RequestHandler):
  """Lists all Users. In addition displays links for editing user info,
     viewing user's friends and adding new users."""

  def get(self):
    users = datastore.User.all()
    template_values = {
        'users': users
        }

    path = os.path.join(os.path.dirname(__file__), 'templates', 'users.html')
    self.response.out.write(template.render(path, template_values))


class UserCredentialsForm(djangoforms.ModelForm):
  """Represents django form for entering user's credentials."""

  class Meta:
    model = datastore.UserCredentials


class UserCredentialsInsertPage(webapp.RequestHandler):
  """Inserts user credentials. GET shows a blank form, POST processes it."""

  def get(self):
    self.response.out.write('<html><body>'
                            '<form method="POST" '
                            'action="/add_credentials">'
                            '<table>')
    # This generates our shopping list form and writes it in the response
    self.response.out.write(UserCredentialsForm())
    self.response.out.write('</table>'
                            '<input type="submit">'
                            '</form></body></html>')

  def post(self):
    data = UserCredentialsForm(data=self.request.POST)
    if data.is_valid():
      # Save the data, and redirect to the view page
      entity = data.save(commit=False)
      entity.put()
      self.redirect('/users')
    else:
      # Reprint the form
      self.response.out.write('<html><body>'
                              '<form method="POST" '
                              'action="/add_credentials">'
                              '<table>')
      self.response.out.write(data)
      self.response.out.write('</table>'
                              '<input type="submit">'
                              '</form></body></html>')


class UserFriendsForm(djangoforms.ModelForm):
  """Represents django form for entering user's friends."""

  class Meta:
    model = datastore.UserFriends
    exclude = ['deleted', 'username']


class UserFriendsInsertPage(webapp.RequestHandler):
  """Inserts user's new friends. GET shows a blank form, POST processes it."""

  def get(self):
    user = self.request.get('user')
    self.response.out.write('<html><body>'
                            '<form method="POST" '
                            'action="/add_friend">'
                            '<table>')
    # This generates our shopping list form and writes it in the response
    self.response.out.write(UserFriendsForm())
    self.response.out.write('</table>'
                            '<input type = hidden name = "user" value = "%s">'
                            '<input type="submit">'
                            '</form></body></html>' % user)

  def post(self):
    data = UserFriendsForm(data=self.request.POST)
    if data.is_valid():
      user = self.request.get('user')
      # Save the data, and redirect to the view page
      entity = data.save(commit=False)
      entity.username = user
      query = datastore.UserFriends.all()
      query.filter('username = ', user)
      query.filter('friend_handle = ', entity.friend_handle)
      result = query.get()
      if result:
	result.deleted = False
	result.updated = datetime.datetime.utcnow()
	result.put()
      else:
        entity.deleted = False
        entity.put()
      self.redirect('/user_friends?user=' + user)
    else:
      # Reprint the form
      self.response.out.write('<html><body>'
                              '<form method="POST" '
                              'action="/add_friend">'
                              '<table>')
      self.response.out.write(data)
      self.response.out.write('</table>'
                              '<input type="submit">'
                              '</form></body></html>')


class UserFriendsListPage(webapp.RequestHandler):
  """Lists all friends for a user. In addition displays links for removing
     friends and adding new friends."""

  def get(self):
    user = self.request.get('user')
    query = datastore.UserFriends.all()
    query.filter('deleted = ', False)
    query.filter('username = ', user)
    friends = query.fetch(50)
    template_values = {
        'friends': friends,
        'user': user
        }
    path = os.path.join(os.path.dirname(__file__),
                        'templates', 'view_friends.html')
    self.response.out.write(template.render(path, template_values))


class DeleteFriendPage(webapp.RequestHandler):
  """Processes delete friend request."""

  def get(self):
    user = self.request.get('user')
    friend = self.request.get('friend')
    query = datastore.UserFriends.all()
    query.filter('username =', user)
    query.filter('friend_handle =', friend)
    result = query.get()
    result.deleted = True
    result.updated = datetime.datetime.utcnow()
    result.put()

    self.redirect('/user_friends?user=' + user)


def main():
  application = webapp.WSGIApplication(
      [('/add_user', UserInsertPage),
       ('/users', UsersListPage),
       ('/add_credentials', UserCredentialsInsertPage),
       ('/add_friend', UserFriendsInsertPage),
       ('/user_friends', UserFriendsListPage),
       ('/delete_friend', DeleteFriendPage),
       ('/edit_user', UserEditPage)
      ],
      debug=True)
  wsgiref.handlers.CGIHandler().run(application)

if __name__ == '__main__':
  main()