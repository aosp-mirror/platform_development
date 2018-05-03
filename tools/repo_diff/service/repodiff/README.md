# To Setup Application:

1.  It is assumed that the Go runtime has been installed with a properly set
    $GOPATH
2.  Run `make bootstrap`

# To Provision a Database

**Use Google Cloud**. This is one assumption made based on the idea that Google
Data Studio will visualize the data.

1.  Log in to [the Google Cloud Platform](https://pantheon.corp.google.com)
2.  Under the **Storage** section, navigate to **SQL**
3.  Click **Create Instance**, select **MySQL** and hit **Next**
4.  Choose **MySQL Second Generation** (the default)
5.  Set Instance ID and root password, then click **Create**
6.  Wait for the instance to initialize, then navigate to **databases** and then
    click **create database**; choose the defaults and make note of the chosen
    database name.
7.  Navigate to **Users** and **Create user account** for the purpose of
    creating a non-root user to log into the database; Make note of the username
    and password chosen.

The application assumes that both a development and production environment
exist, therefore the above steps will need to be completed a second time. Now
the following environment variables will need to be set in your application
environment:

*   GCP_DB_INSTANCE_CONNECTION_NAME_DEV: The instance ID of the provisioned
    database server; this is listed on the SQL homepage in Google Cloud
    alongside the respective database instance
*   GCP_DB_NAME_DEV: the database name created from step 6
*   GCP_DB_USER_DEV: the username created from step 7
*   GCP_DB_PASSWORD_DEV: the password created from step 7
*   GCP_DB_PROXY_PORT_DEV: an arbitrary, unique port used for the local secure
    MySQL proxy

The same environment variables should be set for production. The names are the
same but replace **"DEV"** with **"PROD"**

One the environment variables are set, the database can be readied by running:

`make db_upgrade`

To run the same set of upgrades for production, run:

`ROLE="prod" make db_upgrade`

# Running the Application

Executing the application will by default:

1.  Clone all of the repositories specified in config.json and write the
    observed differentials to the database
2.  Run application-specified analytics on the persisted data and write the
    output to designated denormalized tables; those tables are tightly coupled
    to Datastudio data sources in the [AOSP Branch
    Differentials](https://datastudio.google.com/c/u/0/org/UTgoe29uR0C3F1FBAYBSww/reporting/1lTzMXHBWiVvc0Dnb2DJvRQeTVCAIuvGF/page/9rqP)
3.  Generate CSV reports based on persisted data

Applications steps can be optionally disabled for presumably one-off runs:
`ROLE="prod" ./repodiff
[--execute-diff=false][--denormalize-data=false][--generate-report=false]`

`config.json` will specify input and output parameters for the application;
update to reflect your own environment.

To run:

`make run`

For production: `ROLE="prod" make run`

# Tooling

To connect to the MySQL server used by the application, run:

`make db_shell`

To create new, canonical migration scripts to update the database schema, run:

`make sql_script`

To undo the latest database migration, run:

`make db_downgrade`

To upgrade to the latest database version, run:

`make db_upgrade`

To run tests, run:

`make test`

# Consuming the Application Output

TLDR: Use [Google Data Studio](https://datastudio.google.com); Your datasource
will be tables from the provisioned database set up in the provisioning
instructions. All intended consumable tables are prefixed with
**denormalized_view_**

The rationale behind the application is that setup in Data Studio should require
little to no learning curve (for developers especially), and views are simple
projections of the underlying model. The application, then, should run whatever
necessary logic to produce desired analytics that can be written to a
denormalized database table for straightforward consumption.
