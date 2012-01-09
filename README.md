<!-- -*- mode: markdown; mode: auto-fill; -*- -->

# clj-zentrope-wq (Trivial Managed Concurrent Work Queues) 0.1.0

## Introduction

Zentrope WQ is a convenience library providing a name space for
managing queues and workers all in one place so that the rest of your
code doesn't have to.

## Install

To install via Leiningen:

```clojure
[org.clojars.zentrope/zentrope.wq "0.1.0"]
```

This should pull in everything you need. (NOTE: This lib uses
`clojure.tools.logging` for status information, so you might want to
make sure you're using something compatible with that.)

## Rationale

Provides a name space for managing queues and workers all in one place
so that the rest of your code doesn't have to. I've found this pattern
useful in several of my apps (backend-processing, batch-job kinds of
things) and so I thought I'd put it up in Github in case it's useful
to others, but also to make it more easily distributed across my own
apps.

The idea is that you declare named queues, assign workers to them,
then start "putting" values in the queues to be processed by the
workers (each running in a thread).

Workers might (for instance), do some processing on a job, the put the
job in other queues for other workers, thus creating a pipeline of
sorts in which jobs are processed in parallel.

Because the queues are managed by a process inside a single name
space, your app doesn't have to manage queue and thread resources and
can, instead, focus on the problem any given worker needs to solve.

This lib does nothing new, it just makes the old stuff a quantum
easier.

A few other assumptions:

 * This lib assumes workers exist to produce side-effects (query
   databases, write to file systems, pull or push network resources,
   send messages, etc). Otherwise, much more focused and functional
   patterns seem more appropriate.

 * Workers will keep on working despite exceptions unless an
   `InterruptedException` is thrown.

 * Blocking Queues: As of this writing, I use the `ArrayBlockingQueue`
   data structure from `java.util.concurrent` for a couple of reasons:

     - The GC behavior works much better than the other similar
       structures such that an overloaded system won't blow out memory.

     - The queue sized is a fixed value such that a process putting a
       value into the queue will block if the queue is full. This is
       what I (at least) when you assume an infinite stream of jobs
       flowing through the system from (say) a gigantic network query.

I think that about covers it.

## Usage

Three workers (each in an individual thread) will eat from a queue and
print the value:

```clojure
(:require zentrope-wq.worker-queues :as wq)

(def num-workers 3)

(defn worker-fn
  [value]
  (println "value:" value))

(wq/start :my-queue num-workers worker-fn)

(doseq [v (range 100)]
  (wq/put :my-queue v))

(wq/stop :my-queue)
```

## Examples

Check
[here](https://github.com/zentrope/clj-zentrope-wq/tree/master/src/zentrope_wq/examples)
for examples:

  * [/src/zentrope_wq/examples](https://github.com/zentrope/clj-zentrope-wq/tree/master/src/zentrope_wq/examples)

Really, though, there's nothing more to it than the "Usage" example above.

## Road Map

 * Maybe change start/stop to create!/destroy! or something more
   Clojure-like, and less Erlang-like.

 * How about a function passed in with a queue that gets invoked
   when there's an exception? Similar to the "agent" API.

 * Make sure things are shutdown properly by adding JVM shutdown
   hooks.

 * Configurable queue size. (Oops!)

 * Re-implement queue / worker logic using Executor thread pools
   rather than my own hacks.

## License

Copyright &copy; 2012 Keith Irwin

Distributed under the Eclipse Public License, the same as Clojure.
