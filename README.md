<!-- -*- mode: markdown; mode: auto-fill; -*- -->

# clj-zentrope.wq

Workers and queues managed as a self-contained resource rather than a
data structure and functions. Possibly more like a framework than a
library. Possibly not.

Not ready until I fix up this documentation. Regardless, this is a
narrowly focussed library which might only really work for me.

## Usage

    ```clojure
    (:require zentrope.wq :as wq)

    (wq/start :my-queue 3 (fn [value] (println value)))

    (doseq [v (range 100)]
      (wq/put :my-queue v))

    (wq/stop :my-queue)
    ```

## Notes

To be integrated into a reasonably brief README:

 * Problem: Single queue, many workers, lots of side-effects. Also,
   maybe too many jobs for a given JVM, so need to "block" when
   putting in to a queue.

 * Itch: I don't like to have a lot of "queue/worker" logic repeated
   across a lot of name spaces. Can't I just "send a message" to a
   name space and have it do the right thing?

 * Idea: Leave the management of all the queues to a single name
   space. Just hand it an identifier, the number of workers and a
   worker-function. You can then "put" stuff to that queue, and stop
   the queue, using that identifier.

 * Motivation: I generally use this to create a chain of queues around
   which buzz workers. Each queue/worker combo does something to a
   job, then passes it along to the next queue, if possible. (There's
   no DSL for stiching chains together. These jobs usually involve
   writing to the file system, querying data stores, uploading to
   Amazon S3, etc, etc. Lots of nasty side effects, all of which might
   fail at any given time. Mostly, I want to be able to tune each
   stage as to the number of workers (lots for uploads, a few for DB
   access, etc).

Really just a convenience module, I think.

## Roadmap

Before initial clojars release:

 * Finish this readme.

 * Better documented examples.

 * Make sure things are shutdown properly by adding JVM shutdown hooks.

When I can:

 * Re-implement queue / worker logic using Executor thread pools
   rather than my own hacks.

## License

Copyright &copy; 2012 Keith Irwin

Distributed under the Eclipse Public License, the same as Clojure.
