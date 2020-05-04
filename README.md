# Webact

Run actions from the web. Webact allows to manage scripts on the
server and execute them.

## Concept

You can put executable scripts on the server, either manually or via
the web interface. Then you can execute these scripts via the web
interface or they can be executed periodically based on a schedule.

The scripts may use the following conventions:

1. There are two lines, where one contains `<webact>` and the other
   `</webact>`. All lines in between are considered webact meta data.
2. The webact meta data consists of two sections: key-value pairs and
   a description text separated by an empty line.
3. The description text can be markdown formatted.
4. The scripts either take zero or more input files as arguments.

Example:
``` bash
#!/usr/bin/env bash

# Just a comment not related to webact.
#
# <webact>
# Schedule: 2019-*-* 8,12:0,15,30,45
# Category: Misc
#
# This script only prints the line `hello` to stdout.
# </webact>

echo "hello"
```

While the above is a bash script, it can be written in any other
language, only restricted to what is available on the server. The
webact data is inside a comment and should be near the top of the file
(the first some kilobytes of a script are read).

Since webact parses the files, binary executables are not
possible. But it is usually easy to wrap them in a script.

There are the following recognized key/values:

- `Schedule` a schedule in `yyyy-mm-dd hh:mm` format, where each part
  may be a `*` to indicate all possible values or a comma separated
  list of possible values. See the section below for more details.
- `Enabled` either `true` or `false`. If `false`, the script will not
  be executed.
- `NotifyMail` e-mail addresses to notifiy with the output of the
  script run. It can appear multiple times.
- `NotifyErrorMail` e-mail addresses to notify with the output of
  the script if the return code is not successful. It can appear
  multiple times.
- `NotifySubject` a custom subject for the notification message.
- `Category` for grouping the scripts into categories for a nicer
  view. It can appear multiple times.
- `SuccessCode` a return codes to consider successful. By default a
  return code of `0` is considered success. It can appear multiple
  times to allow multiple successful return codes.
- `Param` this can be used as a structured documentation hint to users
  about what parameters the script expects. It can occur multiple
  times. It can be just a format type, one of `Line`, `Text`,
  `Password`,`File` or `Files`. This allows the web client to create a
  meaningful default form. Since parameters can have a name, you can
  put the parameter name in front separated by an equals sign, for
  example `yourGpgKey=Password`. Currently the only supported input
  formats are `Line` and `Text` for single- and multi line text,
  `Password` for a password input and `File` or `Files` for upload
  files. The web client renders the appropriate controls.

## Running a script

A script can either be executed by sending a http request to its url
or automatically from a schedule. When exectued from a schedule, a
script is executed without any arguments. More arguments can be
supplied by using a `POST` request.

All arguments to a script are provided as files. That is, the scripts
arguments are just file names. The script then can read or ignore
them.

If the scripts is executed within an http request, there is a default
argument containing request header data: uri, query parameters and the
headers all as a JSON file.

Then, if the request is a `multipart/form-data` request, all parts are
stored into separate files, otherwise the whole request body is stored
in one file. These files are passed as arguments to the script,
whereas the request-data file is passed as first argument. If you need
to access the part names of a multipart requests, you can check the
file name: each file name is the sha256 of its contents and optionally
appended with a part name or filename if possible (separated by a
dash). This name is base64 encoded. So for example:

```
01ba4719c80b6fe911b091a7c05124b64eeece964e09c058ef8f9805daca546b-c29tZW5hbWUK
```

The above shows the sha256 sum of the empty string followed by a dash,
followed by the base64 encoding of the string `somename`.

So, if you remove the first 65 characters from a file name and
base64-decode the rest, you'll get the name of the part (which usually
corresponds to the form field name) or an empty string, if there was
no such information available.

The scripts working directory is set to some temporary directory. The
`PATH` variable contains the script directory, so scripts can execute
each other.

### Async vs Sync

Webact main purpose is to manage sets of scripts for (potentially long
running) background execution. Thus the webui starts a script on the
server asynchronously. To get to the output, the user must call
different routes to see the standard output/error.

But it is also possible to synchronously call a script. The standard
output is sent with the response once the script is done. The response
can be controlled via some webact meta data:

- `ContentType` can be specified to let webact use this content type
  instead of the default `text/plain`.
- `BadReturnCode` can be specified multiple times to mark return codes
  as “bad input errors”. Webact will send a 400 (bad request) response
  in this case.
- `SentSdtErr` can be set to `OnError`, `Always` or `Never` (the
  default) to sent the standard error contents instead of the standard
  output.

If a script doesn't return successfully, either a 400 (bad request) or
a 500 (internal server error) response is created. In all cases the
output of the script is added to the response body.

You can call a script synchronously via `GET` and `POST`
requests. Asynchronous execution is only possible via `POST` requests.

## Schedule

Webact allows to run scripts periodically. This is enabled, if the
script meta data contains a valid `Schedule` value. The
[calev](https://github.com/eikek/calev) library is used to parse the
calendar events. See it's documentation for more details. Here some
quick info: The syntax is borrowed from [systemd
timestamps](https://www.freedesktop.org/software/systemd/man/systemd.time.html)
but is not as powerful. It consists of the following:

```
[dow,...] yyyy-mm-dd HH:MM
```

The `dow` (day of week) may be one of: Mon, Tue, Wed, Thu, Fri, Sat,
Sun. This part is optional. It can be a range like `Mon..Fri` or a
list like `Mon,Tue,Thu`. After a space follows the date part,
constisting of the year as a 4-digit value, a 2-digit month and a
2-digit day value. After another space the time part follows with hour
and minute separated by a colon.

Each value (besides dow) can be an asterisk `*` to indicate any
value. If you don't care about a specific weekday, then simply omit
the `dow` list completely. For example, `*` everywhere means to
execute every minute forever:

```
*-*-* *:*
```

To restrict it, use concrete values. For example, this would execute
every hour forever:

```
*-*-* *:00
```

Besides concrete values and a `*`, a comma separated list of values is
possible, a range (e.g. `8..16`) and also repetition values (e.g.
`1/4` is `1,5,9,13,…`). So in this example, a script would execute at
8am and 4pm every day for the year 2019:

```
2019-*-* 8,16:00
```


## Try it out + more documentation

Besides this README you can try out the REST api by checking out this
project and running:

```
sbt reStart
```

(You must install [sbt](https://scala-sbt.org).) Then point your
browser to

```
http://localhost:8011/app/doc
```

and see a generated REST documentation. The url

```
http://localhost:8011/app/index.html
```

takes you to the webapp.

You can override the default configuration by creating a file
`dev.conf` in the source root directory. You don't need to copy the
whole default configuration, it is ok to just put changed settings
into `dev.conf`.

## Limitations

- The running time of a script is recorded based on its name. If a
  script is executed multiple times in parallel, this time doesn't
  really make sense, since all executions overwrite it.

## Configuration

Webact needs two directories on the server with write access. One
contains all the scripts and another for storing temporary
files. Additionally smtp settings for sending e-mails may be
given. But if your e-mails are possible to send via the MX host of
their domain, you don't need to configure a SMTP server.

Webact can be given a configuration file as its only startup
parameter. Please see [this
documentation](https://github.com/lightbend/config/blob/master/HOCON.md)
for its format.

This is the default config file:

```
webact {
  # A name that is displayed in the web application.
  app-name = "Webact"

  # The directory containing all the scripts.
  #
  # If it is writeable then you can edit and add scripts via the
  # webapp.
  script-dir = "."

  # The directory where webact stores information about the last run
  # of a script. It also serves as the base for the working directory.
  tmp-dir = ${java.io.tmpdir}

  # If this is `true` then the PATH environment variable is inherited
  # from the server process into the environment of each script run.
  #
  # If this is false, only what is mentioned in `extra-path` and the
  # script directory is in PATH.
  inherit-path = true

  # Some directories to append to the PATH environment variable when
  # the script is run.
  extra-path = [ "/bin", "/usr/bin", "/usr/local/bin" ]

  # Additional environment settings for each script run.
  env = {

  }

  # If true, monitors the directory containing the scripts for
  # external changes and reloads them in order to apply schedule
  # changes.
  monitor-scripts = true

  # The web server binds to this address.
  bind {
    host = "0.0.0.0"
    port = 8011
  }

  # SMTP settings used to send notification mails.  This is only
  # necessary to fully supply if you send mails to arbirtrary
  # mailboxes. If, for example, you only need to send to yourself,
  # just add your mail as the `sender` below. It most cases this
  # should suffice.
  smtp {
    host = ""
    port = 0
    user = ""
    password = ""
    start-tls = false
    use-ssl = false
    sender = "noreply@localhost"
  }
}
```

## TODO

- git support
- order listing by output state (errors first, then last run date,
  then last modified)
