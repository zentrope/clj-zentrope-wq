<!-- -*- mode: markdown; mode: auto-fill; -*- -->

# clj-zentrope-wq (Trivial Managed Concurrent Work Queues) 0.2.0

## Introduction

Zentrope WQ is a convenience library providing a module for managing
queues and workers all in one place so that the rest of your code
doesn't have to.

## Install

To install via Leiningen:

```clojure
[org.clojars.zentrope/zentrope.wq "0.2.0"]
```

This should pull in everything you need. (NOTE: This lib uses
`clojure.tools.logging` for status information, so you might want to
make sure you're using something compatible with that.)

## Usage

Three workers (each in an individual thread) will eat from a queue and
print the value:

```clojure
(:require zentrope-wq.worker-queues :as wq)

(def num-workers 3)

(defn worker-fn
  [value]
  (println "value:" value))

(defn worker-err
  [qname value exception]
  (println "error on queue" qname "for value" value "exception" exception))

(wq/start :my-queue num-workers worker-fn :qsize 100 :error-handler worker-err)

(doseq [v (range 100)]
  (wq/put :my-queue v))

(wq/stop :my-queue)
```

## Rationale

Provides a module for managing queues and workers all in one place so
that the rest of your code doesn't have to. Kind of a framework with
callbacks sort of thing. I've found this pattern useful in several of
my apps (backend-processing, batch-job kinds of things) so I thought
I'd put it up in Github in case it's useful to others and to
make it more easily distributed across my own apps.

The idea is that you declare named queues, assign workers and an
error-handler to them, then start "putting" values in the queues to be
processed by the thread-based workers.

Workers might (for instance), do some processing on a job, then put
the job in another queue for other workers, thus creating a pipeline
of sorts in which jobs are processed in parallel.

Because the queues are managed by a process inside a single module,
your app doesn't have to manage queue and thread resources and can,
instead, focus on the problem any given worker needs to solve.

This lib does nothing new, it just makes the old stuff a quantum
easier.

A few other assumptions:

 * This lib assumes workers exist to produce side-effects (query
   databases, write to file systems, pull or push network resources,
   send messages, etc). Otherwise, much more focused and functional
   patterns built in to Clojure seem more appropriate.

 * Workers will keep on working despite exceptions unless an
   `InterruptedException` is thrown. If an `error-handler` has been
   associated with the queue, the lib will delegate the qname, value
   and exception to that handler so you can do whatever housekeeping
   you need to do.

 * Blocking Queues: As of this writing, I use the `ArrayBlockingQueue`
   data structure from `java.util.concurrent` for a couple of reasons:

     - The GC behavior works much better than the other similar
       structures such that an overloaded system won't blow out
       memory.

     - The queue size is fixed such that a process putting a value
       into the queue will block if the queue is full. This is what I
       (at least) want when I assume an infinite stream of jobs
       flowing through the system from (say) a gigantic, buffered,
       network query or database cursor.

I think that about covers it.

## Examples

Check
[here](https://github.com/zentrope/clj-zentrope-wq/tree/master/src/zentrope_wq/examples)
for examples:

  * [/src/zentrope_wq/examples](https://github.com/zentrope/clj-zentrope-wq/tree/master/src/zentrope_wq/examples)

Really, though, there's nothing more to it than the "Usage" example above.

## Road Map

 * Maybe change start/stop to create!/destroy! or something more
   Clojure-like, and less Erlang-like.

 * <strike>How about a function passed in with a queue that gets invoked
   when there's an exception? Similar to the "agent" API.</strike> (0.2.0)

 * Make sure things are shutdown properly by adding JVM shutdown
   hooks.

 * <strike>Configurable queue size. (Oops!)</strike> (0.2.0)

 * Re-implement queue / worker logic using Executor thread pools
   rather than my own hacks.

## History

  * **0.1.0** Intial version.

  * **0.2.0** Added additional parameters for configuration the queue
    size and an error handler if the worker throws an exception.

## License

Copyright &copy; 2012 Keith Irwin

Distributed under the Eclipse Public License, the same as Clojure.
