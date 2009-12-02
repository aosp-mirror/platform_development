#!/usr/bin/python
#
# Copyright 2007 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
An interactive, stateful AJAX shell that runs Python code on the server.

Part of http://code.google.com/p/google-app-engine-samples/.

May be run as a standalone app or in an existing app as an admin-only handler.
Can be used for system administration tasks, as an interactive way to try out
APIs, or as a debugging aid during development.

The logging, os, sys, db, and users modules are imported automatically.

Interpreter state is stored in the datastore so that variables, function
definitions, and other values in the global and local namespaces can be used
across commands.

To use the shell in your app, copy shell.py, static/*, and templates/* into
your app's source directory. Then, copy the URL handlers from app.yaml into
your app.yaml.

TODO: unit tests!
"""

import logging
import new
import os
import pickle
import sys
import traceback
import types
import wsgiref.handlers

from google.appengine.api import users
from google.appengine.ext import db
from google.appengine.ext import webapp
from google.appengine.ext.webapp import template


# Set to True if stack traces should be shown in the browser, etc.
_DEBUG = True

# The entity kind for shell sessions. Feel free to rename to suit your app.
_SESSION_KIND = '_Shell_Session'

# Types that can't be pickled.
UNPICKLABLE_TYPES = (
  types.ModuleType,
  types.TypeType,
  types.ClassType,
  types.FunctionType,
  )

# Unpicklable statements to seed new sessions with.
INITIAL_UNPICKLABLES = [
  'import logging',
  'import os',
  'import sys',
  'from google.appengine.ext import db',
  'from google.appengine.api import users',
  ]


class Session(db.Model):
  """A shell session. Stores the session's globals.

  Each session globals is stored in one of two places:

  If the global is picklable, it's stored in the parallel globals and
  global_names list properties. (They're parallel lists to work around the
  unfortunate fact that the datastore can't store dictionaries natively.)

  If the global is not picklable (e.g. modules, classes, and functions), or if
  it was created by the same statement that created an unpicklable global,
  it's not stored directly. Instead, the statement is stored in the
  unpicklables list property. On each request, before executing the current
  statement, the unpicklable statements are evaluated to recreate the
  unpicklable globals.

  The unpicklable_names property stores all of the names of globals that were
  added by unpicklable statements. When we pickle and store the globals after
  executing a statement, we skip the ones in unpicklable_names.

  Using Text instead of string is an optimization. We don't query on any of
  these properties, so they don't need to be indexed.
  """
  global_names = db.ListProperty(db.Text)
  globals = db.ListProperty(db.Blob)
  unpicklable_names = db.ListProperty(db.Text)
  unpicklables = db.ListProperty(db.Text)

  def set_global(self, name, value):
    """Adds a global, or updates it if it already exists.

    Also removes the global from the list of unpicklable names.

    Args:
      name: the name of the global to remove
      value: any picklable value
    """
    blob = db.Blob(pickle.dumps(value))

    if name in self.global_names:
      index = self.global_names.index(name)
      self.globals[index] = blob
    else:
      self.global_names.append(db.Text(name))
      self.globals.append(blob)

    self.remove_unpicklable_name(name)

  def remove_global(self, name):
    """Removes a global, if it exists.

    Args:
      name: string, the name of the global to remove
    """
    if name in self.global_names:
      index = self.global_names.index(name)
      del self.global_names[index]
      del self.globals[index]

  def globals_dict(self):
    """Returns a dictionary view of the globals.
    """
    return dict((name, pickle.loads(val))
                for name, val in zip(self.global_names, self.globals))

  def add_unpicklable(self, statement, names):
    """Adds a statement and list of names to the unpicklables.

    Also removes the names from the globals.

    Args:
      statement: string, the statement that created new unpicklable global(s).
      names: list of strings; the names of the globals created by the statement.
    """
    self.unpicklables.append(db.Text(statement))

    for name in names:
      self.remove_global(name)
      if name not in self.unpicklable_names:
        self.unpicklable_names.append(db.Text(name))

  def remove_unpicklable_name(self, name):
    """Removes a name from the list of unpicklable names, if it exists.

    Args:
      name: string, the name of the unpicklable global to remove
    """
    if name in self.unpicklable_names:
      self.unpicklable_names.remove(name)


class FrontPageHandler(webapp.RequestHandler):
  """Creates a new session and renders the shell.html template.
  """

  def get(self):
    # set up the session. TODO: garbage collect old shell sessions
    session_key = self.request.get('session')
    if session_key:
      session = Session.get(session_key)
    else:
      # create a new session
      session = Session()
      session.unpicklables = [db.Text(line) for line in INITIAL_UNPICKLABLES]
      session_key = session.put()

    template_file = os.path.join(os.path.dirname(__file__), 'templates',
                                 'shell.html')
    session_url = '/?session=%s' % session_key
    vars = { 'server_software': os.environ['SERVER_SOFTWARE'],
             'python_version': sys.version,
             'session': str(session_key),
             'user': users.get_current_user(),
             'login_url': users.create_login_url(session_url),
             'logout_url': users.create_logout_url(session_url),
             }
    rendered = webapp.template.render(template_file, vars, debug=_DEBUG)
    self.response.out.write(rendered)


class StatementHandler(webapp.RequestHandler):
  """Evaluates a python statement in a given session and returns the result.
  """

  def get(self):
    self.response.headers['Content-Type'] = 'text/plain'

    # extract the statement to be run
    statement = self.request.get('statement')
    if not statement:
      return

    # the python compiler doesn't like network line endings
    statement = statement.replace('\r\n', '\n')

    # add a couple newlines at the end of the statement. this makes
    # single-line expressions such as 'class Foo: pass' evaluate happily.
    statement += '\n\n'

    # log and compile the statement up front
    try:
      logging.info('Compiling and evaluating:\n%s' % statement)
      compiled = compile(statement, '<string>', 'single')
    except:
      self.response.out.write(traceback.format_exc())
      return

    # create a dedicated module to be used as this statement's __main__
    statement_module = new.module('__main__')

    # use this request's __builtin__, since it changes on each request.
    # this is needed for import statements, among other things.
    import __builtin__
    statement_module.__builtins__ = __builtin__

    # load the session from the datastore
    session = Session.get(self.request.get('session'))

    # swap in our custom module for __main__. then unpickle the session
    # globals, run the statement, and re-pickle the session globals, all
    # inside it.
    old_main = sys.modules.get('__main__')
    try:
      sys.modules['__main__'] = statement_module
      statement_module.__name__ = '__main__'

      # re-evaluate the unpicklables
      for code in session.unpicklables:
        exec code in statement_module.__dict__

      # re-initialize the globals
      for name, val in session.globals_dict().items():
        try:
          statement_module.__dict__[name] = val
        except:
          msg = 'Dropping %s since it could not be unpickled.\n' % name
          self.response.out.write(msg)
          logging.warning(msg + traceback.format_exc())
          session.remove_global(name)

      # run!
      old_globals = dict(statement_module.__dict__)
      try:
        old_stdout = sys.stdout
        old_stderr = sys.stderr
        try:
          sys.stdout = self.response.out
          sys.stderr = self.response.out
          exec compiled in statement_module.__dict__
        finally:
          sys.stdout = old_stdout
          sys.stderr = old_stderr
      except:
        self.response.out.write(traceback.format_exc())
        return

      # extract the new globals that this statement added
      new_globals = {}
      for name, val in statement_module.__dict__.items():
        if name not in old_globals or val != old_globals[name]:
          new_globals[name] = val

      if True in [isinstance(val, UNPICKLABLE_TYPES)
                  for val in new_globals.values()]:
        # this statement added an unpicklable global. store the statement and
        # the names of all of the globals it added in the unpicklables.
        session.add_unpicklable(statement, new_globals.keys())
        logging.debug('Storing this statement as an unpicklable.')

      else:
        # this statement didn't add any unpicklables. pickle and store the
        # new globals back into the datastore.
        for name, val in new_globals.items():
          if not name.startswith('__'):
            session.set_global(name, val)

    finally:
      sys.modules['__main__'] = old_main

    session.put()


def main():
  application = webapp.WSGIApplication(
    [('/gae_shell/', FrontPageHandler),
     ('/gae_shell/shell.do', StatementHandler)], debug=_DEBUG)
  wsgiref.handlers.CGIHandler().run(application)


if __name__ == '__main__':
  main()
