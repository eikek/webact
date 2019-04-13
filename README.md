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
4. The scripts either take no input or one input which is a file.

Example:
``` bash
#!/usr/bin/env bash

# Just a comment not related to webact.
#
# <webact>
# Schedule: 2019-*-* 12:0,10,20,30,40,50
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
  list of possible values.
- `Enabled` either `true` or `false`. If `false`, the script will not
  be executed.
- `Notify-Mail` one or more (comma separated) e-mail addresses to
  notifiy with the output of the script run.
- `Notify-Error-Mail` one or more (comma separated) e-mail addresses
  to notify with the output of the script if the return code is not
  `0`.
- `Category` for grouping the scripts into categories for a nicer
  view.
- `SuccessCodes` a comma separated list of return codes to consider
  successful. By default a return code of `0` is considered success.

## Running a script

A script can either be executed by sending a http request to its url
or automatically from a schedule. Using the `GET` method, a script is
executed with the request data as argument. When exectued from a
schedule, a script is executed without any arguments. More arguments
can be supplied by using a `POST` request.

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

## Configuration

Webact needs two directories on the server with write access. One
contains all the scripts and another for storing temporary
files. Additionally smtp settings for sending e-mails may be
given. But if your e-mails are possible to send via the MX host of
their domain, you don't need to configure a SMTP server.

Webact can be given a configuration file.
