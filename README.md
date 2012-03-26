pummel(1) -- http load testing tool
===================================

## SYNOPSIS

`pummel step` [<var>options</var>] [<var>url-file</var>]

`pummel limit` [<var>options</var>] [<var>url-file</var>]

`pummel benchmark` [<var>options</var>] [<var>url-file</var>]

`pummel analyze`

`pummel help [<var>command</var>]`

## DESCRIPTION

Pummel is an HTTP load generation and measurement tool. It is designed
to help find the concurrency limits of an HTTP server relative to a
specified latency latency at a given percentile. For instance, the
incantation:
    pummel limit --percentile 99.9 --target 50 ./urls.txt
would search for the highest concurrency level which will keep the
99.9th percentile response times below 50 milliseconds.

Alternately, pummel may be used for exploratory testing. The step
command is particularly useful for his, such as:
    pummel step --percentile 99.9 --step 'c * 2' ./urls.txt
will double the concurrency for each run, printing the concurrency
level, latency at the specified percentile, mean latency, and requests
per second at that concurrency level.

## FILES

Pummel takes as input a sequence of URLs, one per line, either on
standard input or specified in a urls file as a command line
argument. It will build a FIFO queue of requests which will be
pulled off and executed concurrently by the program.

## COMMANDS

### limit

The `limit` command takes a list of urls from either standard in or
from a file specifed as an argument to the command.

* `-l`, `--labels`:
  Print column labels in the output
* `-m` <var>max-requests</var>, `--max` <var>max-requests</var>:
  Maximum number of requests to execute

* `-p` <var>percentile</var>, `--percentile` <var>percentile</var>:
  Percentile to try to target, default is 99th percentile

* `-s` <var>start</var>, `--start` <var>start</var>:
  Initial concurrency level, defaults to 100

* `-t` <var>target</var>, `--target` <var>target</var>:
  Target 99th percentile threshold, default is 100
  
### step

The `step` command takes a list of urls from either standard in or
from a file specifed as an argument to the command.

* `-l`, `--labels`:
  Show column labels

* `-L` <var>limit</var>, `--limit` <var>limit</var>:
concurrency limit to stop at, default is 2147483647

* `-m` <var>max-requests</var>, `--max-requests` <var>max-requests</var>:
  Maximum number of requests to execute

* `-p` <var>percentile</var>, `--percentile` <var>percentile</var>:
  Percentile to try to target, default is 99th percentile

* `-s` <var>start</var>, `--start` <var>start</var>:
  initial concurrency level, defaults to 100

* `--step` <var>step-function</var>: 
  Expression used to calculate the next concurrency level. The default
  is `c + 1` which increments the concurrency level by 1. It accepts
  either an [MVEL Expression](http://mvel.codehaus.org/) or
  [Clojure fucntion](http://clojure.org/) function. In the MVEL case
  the input level is contained in a variable named `c`, in the clojure
  case it will be the lone argument to the function.

### benchmark

The `benchmark` command takes a list of urls from either standard in or
from a file specifed as an argument to the command.

* `-c` <var>concurrency</var>, `--concurrency` <var>concurrency</var>:
  Concurrency -- how many requests to keep in flight at once

* `-m` <var>maxRequests</var>, `--max` <var>maxRequests</var>:
  Maximum number of requests to execute

* `-r`, `--report`:
  report basic stats on stderr when finished:

### analyze

The `analyze` command takes a list of times, in millis, one per line
on standard input. This is useful in combination with the `benchmark`
command.

## COPYRIGHT

Pummel is Copyright (C) 2012 Ning, Inc. 

Pummel is available under the
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)


[SYNOPSIS]: #SYNOPSIS "SYNOPSIS"
[DESCRIPTION]: #DESCRIPTION "DESCRIPTION"
[FILES]: #FILES "FILES"
[COMMANDS]: #COMMANDS "COMMANDS"
[limit]: #limit "limit"
[step]: #step "step"
[benchmark]: #benchmark "benchmark"
[analyze]: #analyze "analyze"
[COPYRIGHT]: #COPYRIGHT "COPYRIGHT"


[pummel(1)]: pummel.1.html
