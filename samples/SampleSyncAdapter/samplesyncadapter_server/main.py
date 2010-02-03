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

"""Handlers for Sample SyncAdapter services.

Contains several RequestHandler subclasses used to handle post operations.
This script is designed to be run directly as a WSGI application.

  Authenticate: Handles user requests for authentication.
  FetchFriends: Handles user requests for friend list.
  FriendData: Stores information about user's friends.
"""

import cgi
from datetime import datetime
from django.utils import simplejson
from google.appengine.api import users
from google.appengine.ext import db
from google.appengine.ext import webapp
from model import datastore
import wsgiref.handlers


class Authenticate(webapp.RequestHandler):
  """Handles requests for login and authentication.

  UpdateHandler only accepts post events. It expects each
  request to include username and password fields. It returns authtoken
  after successful authentication and "invalid credentials" error otherwise.
  """

  def post(self):
    self.username = self.request.get('username')
    self.password = self.request.get('password')
    password = datastore.UserCredentials.get(self.username)
    if password == self.password:
      self.response.set_status(200, 'OK')
      # return the password as AuthToken
      self.response.out.write(password)
    else:
      self.response.set_status(401, 'Invalid Credentials')


class FetchFriends(webapp.RequestHandler):
  """Handles requests for fetching user's friendlist.

  UpdateHandler only accepts post events. It expects each
  request to include username and authtoken. If the authtoken is valid
  it returns user's friend info in JSON format.It uses helper
  class FriendData to fetch user's friendlist.
  """

  def post(self):
    self.username = self.request.get('username')
    self.password = self.request.get('password')
    self.timestamp = None
    timestamp = self.request.get('timestamp')
    if timestamp:
      self.timestamp = datetime.strptime(timestamp, '%Y/%m/%d %H:%M')
    password = datastore.UserCredentials.get(self.username)
    if password == self.password:
      self.friend_list = []
      friends = datastore.UserFriends.get_friends(self.username)
      if friends:
        for friend in friends:
          friend_handle = getattr(friend, 'friend_handle')

          if self.timestamp is None or getattr(friend, 'updated') > self.timestamp:
            if (getattr(friend, 'deleted')) == True:
              friend = {}
              friend['u'] = friend_handle
              friend['d'] = 'true'
              friend['i'] = str(datastore.User.get_user_id(friend_handle))
              self.friend_list.append(friend)
            else:
              FriendsData(self.friend_list, friend_handle)
          else:
            if datastore.User.get_user_last_updated(friend_handle) > self.timestamp:
              FriendsData(self.friend_list, friend_handle)
      self.response.set_status(200)
      self.response.out.write(toJSON(self.friend_list))
    else:
      self.response.set_status(401, 'Invalid Credentials')

class FetchStatus(webapp.RequestHandler):
  """Handles requests fetching friend statuses.

  UpdateHandler only accepts post events. It expects each
  request to include username and authtoken. If the authtoken is valid
  it returns status info in JSON format.
  """

  def post(self):
    self.username = self.request.get('username')
    self.password = self.request.get('password')
    password = datastore.UserCredentials.get(self.username)
    if password == self.password:
      self.status_list = []
      friends = datastore.UserFriends.get_friends(self.username)
      if friends:
        for friend in friends:
          friend_handle = getattr(friend, 'friend_handle')
          status_text = datastore.User.get_user_status(friend_handle)
	  user_id = datastore.User.get_user_id(friend_handle)
          status = {}
          status['i'] = str(user_id)
          status['s'] = status_text
          self.status_list.append(status)
      self.response.set_status(200)
      self.response.out.write(toJSON(self.status_list))
    else:
      self.response.set_status(401, 'Invalid Credentials')

  def toJSON(self):
    """Dumps the data represented by the object to JSON for wire transfer."""
    return simplejson.dumps(self.friend_list)


def toJSON(object):
  """Dumps the data represented by the object to JSON for wire transfer."""
  return simplejson.dumps(object)

class FriendsData(object):
  """Holds data for user's friends.

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
  }

  def __init__(self, friend_list, username):
    obj = datastore.User.get_user_info(username)
    friend = {}
    for obj_name, json_name in self.__FIELD_MAP.items():
      if hasattr(obj, obj_name):
        friend[json_name] = str(getattr(obj, obj_name))
        friend['i'] = str(obj.key().id())
    friend_list.append(friend)


def main():
  application = webapp.WSGIApplication(
      [('/auth', Authenticate),
       ('/login', Authenticate),
       ('/fetch_friend_updates', FetchFriends),
       ('/fetch_status', FetchStatus),
      ],
      debug=True)
  wsgiref.handlers.CGIHandler().run(application)

if __name__ == "__main__":
  main()